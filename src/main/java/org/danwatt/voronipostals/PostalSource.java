package org.danwatt.voronipostals;


import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class PostalSource {
	private static final GeometryFactory geometryFactory = new GeometryFactory();
	private static final WKTReader reader = new WKTReader(geometryFactory);
	private static final List<String> MAP_COLORS = Arrays.asList("#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928");
	private static PostalSource instance;
	private Map<String, PostalCode> postalCodes;
	private STRtree postalIndex = new STRtree();
	private STRtree countyIndex = new STRtree();
	private BinaryOperator<Geometry> mergeGeometries = (g1, g2) -> {
		Geometry u = g1.union(g2);
		u.setUserData(g1.getUserData());
		return u;
	};

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

	List<PostalCode> getNearbyCounties(double lat, double lon) {
		Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
		List<Geometry> results = getNeighbors(countyIndex, point.buffer(2));

		List<Tuple2<Geometry, PostalCode>> matches = results.stream().map(geo -> {
			PostalCode pc = new PostalCode();
			pc.state = StringUtils.substringAfterLast((String) geo.getUserData(), ",");
			pc.county = StringUtils.substringBeforeLast((String) geo.getUserData(), ",");
			pc.wkt = geo.toText();
			return Tuple.tuple(geo, pc);
		}).collect(toList());
		color(matches);
		return matches.stream().map(t -> t.v2).collect(toList());
	}

	List<PostalCode> getPostalsAroundPoint(double lat, double lon) {
		Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
		List<Geometry> results = getNeighbors(postalIndex, point.buffer(0.5));

		List<Tuple2<Geometry, PostalCode>> matches = results.stream().map(geo -> Tuple.tuple(geo, postalCodes.get(geo.getUserData()).clone())).collect(toList());
		color(matches);

		return matches.stream().map(Tuple2::v2).collect(Collectors.toList());
	}

	public List<PostalCode> getUnion(List<String> strings) {
		Optional<Geometry> unioned = strings.stream().map(postal -> postalCodes.get(postal).wkt).map(wkt -> {
			try {
				return reader.read(wkt);
			} catch (Exception ex) {
				return null;
			}
		}).filter(Objects::nonNull).reduce(Geometry::union);
		return unioned.isPresent() ? Collections.singletonList(unioned.map(geo -> {
			PostalCode pc = new PostalCode();
			pc.wkt = geo.toText();
			pc.color = MAP_COLORS.get(1);
			return pc;
		}).get()) : Collections.emptyList();
	}

	private void buildCounties() {
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
		countyPolygons.values().forEach(g -> countyIndex.insert(g.getEnvelopeInternal(), g));
	}

	//This implements a very naieve n-coloring algorithm. It could be made much faster
	private void color(List<Tuple2<Geometry, PostalCode>> matches) {
		STRtree index = new STRtree(matches.size());
		matches.forEach(t -> index.insert(t.v1.getEnvelopeInternal(), t.v1));
		Map<String, String> colors = new LinkedHashMap<>();
		matches.forEach(t -> {
			List<String> potentialColors = new ArrayList<String>(MAP_COLORS);
			List<Geometry> neighbors = getNeighbors(index, t.v1);
			potentialColors.removeAll(neighbors.stream().map(n -> colors.get(n.getUserData())).filter(Objects::nonNull).collect(Collectors.toSet()));
			colors.put((String) t.v1.getUserData(), potentialColors.get(0));
			t.v2.color = potentialColors.get(0);
		});
	}

	private List<Geometry> getNeighbors(STRtree index, Geometry geometry) {
		return ((List<Geometry>) index.query(geometry.getEnvelopeInternal()))
				.stream()
				.filter(m -> m.intersects(geometry) && m != geometry)
				.collect(toList());
	}

	private void computeVoroni() {
		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		List<Tuple2<PostalCode, Coordinate>> postalToCenter = postalCodes.values()
				.stream()
				.map(pc -> Tuple.tuple(pc, new Coordinate(pc.longitude, pc.latitude)))
				.collect(toList());

		vdb.setSites(postalToCenter.stream().map(Tuple2::v2).collect(toList()));
		GeometryCollection gc = (GeometryCollection) vdb.getDiagram(geometryFactory);
		STRtree si = new STRtree();
		for (int i = 0; i < gc.getNumGeometries(); i++) {
			Geometry geometryN = gc.getGeometryN(i);
			si.insert(geometryN.getEnvelopeInternal(), geometryN);
		}
		postalToCenter.forEach(t -> {
			Point point = geometryFactory.createPoint(t.v2);
			List<Geometry> query = si.query(point.getEnvelopeInternal());
			Optional<Geometry> match = query.stream().filter(point::intersects).findFirst();
			if (match.isPresent()) {
				Geometry geometry = match.get();
				t.v1.wkt = geometry.toText();
				geometry.setUserData(t.v1.postal);
				postalIndex.insert(geometry.getEnvelopeInternal(), geometry);
			}
		});
	}
}
