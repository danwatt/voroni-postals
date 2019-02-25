package org.danwatt.voronipostals;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.*;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;

import java.io.*;

public class Converter {
	public static final GeometryFactory LOW_PRECISION_FACTORY = new GeometryFactory(new PrecisionModel(40));
	public static void main(String[] args) throws Exception {
		WKBWriter writer = new WKBWriter();
		Geometry alaska = loadFile("/Users/daniel.watt/git/oss/voroni-postals/src/main/resources/ak.wkt");
		Geometry canada = loadFile("/Users/daniel.watt/git/oss/voroni-postals/src/main/resources/can.wkt");
		Geometry mexico = loadFile("/Users/daniel.watt/git/oss/voroni-postals/src/main/resources/mex.wkt");
		Geometry us = loadFile("/Users/daniel.watt/git/oss/voroni-postals/src/main/resources/us1.wkt");

		alaska = alaska.buffer(0.2).difference(canada);
		alaska = DouglasPeuckerSimplifier.simplify(alaska, 0.1);
		us = us.buffer(0.0275).difference(canada).difference(mexico);
		us = DouglasPeuckerSimplifier.simplify(us, 0.015);
		us = us.union(alaska);

		Geometry usGeom = LOW_PRECISION_FACTORY.createGeometry(us);
		Geometry simplified = usGeom.buffer(0.0).union();
		simplified = SmallHoleRemover.clean(simplified,100);

		writer.write(simplified,new OutputStreamOutStream(new FileOutputStream("/Users/daniel.watt/git/oss/voroni-postals/src/main/resources/us-simple.wkb")));
		FileWriter fw = new FileWriter("/Users/daniel.watt/git/oss/voroni-postals/src/main/resources/us-simple.wkt");
		new WKTWriter().write(simplified, fw);
		fw.close();;

	}

	private static Geometry loadFile(String path) throws ParseException, IOException {
		return new WKTReader().read(new FileReader(path));
	}
}
