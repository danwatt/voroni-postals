package org.danwatt.voronipostals;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class PostalSource {
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final WKTReader reader = new WKTReader(geometryFactory);
    private Map<String, PostalCode> postalCodes;
    private STRtree postalIndex = new STRtree();
    private STRtree countyIndex = new STRtree();
    private static final List<String> MAP_COLORS = Arrays.asList("#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928");

    private static PostalSource instance;

    static PostalSource getInstance() {
        if (null == instance) {
            synchronized (geometryFactory) {
                if (null == instance) {
                    System.out.println("Loading postal codes");
                    instance = new PostalSource();
                    System.out.println("Loaded " + instance.postalCodes.size() + " postal codes / polgyons");
                }
            }
        }
        return instance;
    }

    private PostalSource() {
        try {
            URL res = ClassLoader.getSystemResource("US.txt");
            Path path;
            if (res.toString().startsWith("file:")) {
                path = Paths.get(res.toURI());
            } else {
                String[] parts = res.toString().split("!");
                FileSystem fs = FileSystems.newFileSystem(URI.create(parts[0]), new HashMap<>());
                path = fs.getPath(parts[1]);
            }
            postalCodes = Files.lines(path).map(PostalCode::loadLine).distinct().collect(Collectors.toMap(pc -> pc.postal, pc -> pc));
            computeVoroni();
            buildCounties();
        } catch (Exception ex) {
            ex.printStackTrace();
            postalCodes = new HashMap<>();
        }
    }

    private void buildCounties() {
        BinaryOperator<Geometry> mergeGeometries = (g1, g2) -> {
            Geometry u = g1.union(g2);
            u.setUserData(g1.getUserData());
            return u;
        };
        Map<String, Geometry> countyPolygons = postalCodes.values().stream().map(pc -> {
            try {
                String cs = pc.county + ", " + pc.state;
                Geometry read = reader.read(pc.wkt);
                read.setUserData(cs);
                return Tuple.tuple(cs, read);
            } catch (ParseException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toMap(t -> t.v1, t -> t.v2, mergeGeometries));
        countyPolygons.values().stream().forEach(g -> countyIndex.insert(g.getEnvelopeInternal(), g));
    }

    List<PostalCode> getNearbyCounties(double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        List<Geometry> results = getNeighbors(countyIndex, point.buffer(2));

        List<Tuple2<Geometry,PostalCode>> matches = results.stream().map(geo -> {
            PostalCode pc = new PostalCode();
            pc.state = StringUtils.substringAfterLast((String) geo.getUserData(),",");
            pc.county = StringUtils.substringBeforeLast((String)geo.getUserData(),",");
            pc.wkt = geo.toText();
            return Tuple.tuple(geo,pc);
        }).collect(toList());
        color(matches);
        return matches.stream().map(t->t.v2).collect(toList());
    }

    private void color(List<Tuple2<Geometry,PostalCode>> matches) {
        STRtree index = new STRtree(matches.size());
        matches.stream().forEach(t->index.insert(t.v1.getEnvelopeInternal(),t.v1));
        Map<String,String> colors = new LinkedHashMap<>();
        matches.stream().forEach(t->{
            List<String> potentialColors = new ArrayList<String>(MAP_COLORS);
            List<Geometry> neighbors = getNeighbors(index,t.v1);
            potentialColors.removeAll(neighbors.stream().map(n->colors.get(n.getUserData())).filter(Objects::nonNull).collect(Collectors.toSet()));
            colors.put((String) t.v1.getUserData(),potentialColors.get(0));
            t.v2.color = potentialColors.get(0);
        });
    }

    private List<Geometry> getNeighbors(SpatialIndex index, Geometry geometry) {
        List<Geometry> matches = index.query(geometry.getEnvelopeInternal());
        return matches.stream().filter(m->m.intersects(geometry) && m!=geometry).collect(toList());
    }

    List<PostalCode> getPostalsAroundPoint(double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        List<Geometry> results = getNeighbors(postalIndex, point.buffer(0.5));

        List<PostalCode> matches = results.stream().map(geo -> postalCodes.get(geo.getUserData()).clone()).collect(toList());
        Map<String, String> cityStateColors = new TreeMap<>(matches.stream().map(pc -> pc.city + "," + pc.state).distinct().collect(Collectors.toMap(cs -> cs, cs -> "")));
        AtomicInteger ai = new AtomicInteger(0);
        cityStateColors.keySet().stream().forEach(key -> {
                    cityStateColors.put(key, MAP_COLORS.get(ai.getAndIncrement() % MAP_COLORS.size()));
                }
        );
        matches.forEach(pc -> {
            pc.color = cityStateColors.get(pc.city + "," + pc.state);
        });
        return matches;
    }

    private void computeVoroni() {
        VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
        List<Tuple2<PostalCode, Coordinate>> postalToCenter = postalCodes.values().stream().map(pc -> Tuple.tuple(pc, new Coordinate(pc.longitude, pc.latitude))).collect(toList());
        vdb.setSites(postalToCenter.stream().map(Tuple2::v2).collect(toList()));
        Geometry diagram = vdb.getDiagram(geometryFactory);
        GeometryCollection gc = (GeometryCollection) diagram;
        SpatialIndex si = new STRtree();
        for (int i = 0; i < gc.getNumGeometries(); i++) {
            Geometry geometryN = gc.getGeometryN(i);
            si.insert(geometryN.getEnvelopeInternal(), geometryN);
        }
        postalToCenter.stream().forEach(t -> {
            Point point = geometryFactory.createPoint(t.v2);
            List<Geometry> query = si.query(point.getEnvelopeInternal());
            if (!query.isEmpty()) {
                Optional<Geometry> match = query.stream().filter(point::intersects).findFirst();
                if (match.isPresent()) {
                    Geometry geometry = match.get();
                    t.v1.wkt = geometry.toText();
                    geometry.setUserData(t.v1.postal);
                }
            }
        });
    }
}
