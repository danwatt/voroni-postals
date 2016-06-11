package org.danwatt.voronipostals;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.strtree.STRtree;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PostalSource {
    private static final GeometryFactory geometryFactory = new GeometryFactory();
    private static final WKTReader reader = new WKTReader(geometryFactory);
    private Map<String, PostalCode> postalCodes;
    private SpatialIndex voroniIndex = new STRtree();

    private static PostalSource instance;

    public static PostalSource getInstance() {
        if (null == instance) {
            synchronized (geometryFactory) {
                if (null == instance) {
                    System.out.println("Loading postal codes");
                    instance = new PostalSource();
                    System.out.println("Loaded " + instance.postalCodes.size() +" postal codes / polgyons");
                }
            }
        }
        return instance;
    }

    private PostalSource() {
        try {
            postalCodes = Files.lines(Paths.get(ClassLoader.getSystemResource("US.txt")
                    .toURI())).map(PostalCode::loadLine).distinct().collect(Collectors.toMap(pc -> pc.postal, pc -> pc));
            computeVoroni();
        } catch (Exception ex) {
            ex.printStackTrace();
            postalCodes = new HashMap<>();
        }
    }

    public List<PostalCode> getAroundPoint(double lat, double lon) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        Geometry buffer = point.buffer(0.5);
        List<Geometry> results = voroniIndex.query(buffer.getEnvelopeInternal());

        List<String> colors = Arrays.asList("#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928");

        List<PostalCode> matches = results.stream().filter(geo -> geo.intersects(buffer)).map(geo -> postalCodes.get(geo.getUserData()).clone()).collect(Collectors.toList());
        Map<String, String> cityStateColors = new TreeMap<>(matches.stream().map(pc -> pc.city + "," + pc.state).distinct().collect(Collectors.toMap(cs -> cs, cs -> "")));
        AtomicInteger ai = new AtomicInteger(0);
        cityStateColors.keySet().stream().forEach(key -> {
                    cityStateColors.put(key, colors.get(ai.getAndIncrement() % colors.size()));
                }
        );
        matches.forEach(pc -> {
            pc.color = cityStateColors.get(pc.city + "," + pc.state);
        });
        return matches;
    }

    public void computeVoroni() {
        VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
        List<Tuple2<PostalCode, Coordinate>> collect = postalCodes.values().stream().map(pc -> Tuple.tuple(pc, new Coordinate(pc.longitude, pc.latitude))).collect(Collectors.toList());
        List<Coordinate> coords = collect.stream().map(Tuple2::v2).collect(Collectors.toList());
        vdb.setSites(coords);
        Geometry diagram = vdb.getDiagram(geometryFactory);
        GeometryCollection gc = (GeometryCollection) diagram;
        SpatialIndex si = new STRtree();
        for (int i = 0; i < gc.getNumGeometries(); i++) {
            Geometry geometryN = gc.getGeometryN(i);
            si.insert(geometryN.getEnvelopeInternal(), geometryN);
        }
        collect.stream().forEach(t -> {
            Point point = geometryFactory.createPoint(t.v2);
            List<Geometry> query = si.query(point.getEnvelopeInternal());
            if (query.isEmpty()) {
                System.out.println("Could not find a match for " + t.v1.postal);
            } else {
                Optional<Geometry> match = query.stream().filter(geo -> point.intersects(geo)).findFirst();
                if (match.isPresent()) {
                    Geometry geometry = match.get();
                    t.v1.wkt = geometry.toText();
                    geometry.setUserData(t.v1.postal);
                    voroniIndex.insert(geometry.getEnvelopeInternal(), geometry);
                }
            }
        });
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        new PostalSource().computeVoroni();
    }

}
