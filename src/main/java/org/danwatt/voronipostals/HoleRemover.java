package org.danwatt.voronipostals;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.GeometryMapper;

import java.util.ArrayList;
import java.util.List;

public class HoleRemover {

	public interface Predicate {
		boolean value(Geometry geom);
	}

	private Geometry geom;
	private Predicate isRemoved;

	/**
	 * Creates a new hole remover instance.
	 *
	 * @param geom the geometry to process
	 */
	public HoleRemover(Geometry geom, Predicate isRemoved) {
		this.geom = geom;
		this.isRemoved = isRemoved;
	}

	/**
	 * Gets the cleaned geometry.
	 *
	 * @return the geometry with matched holes removed.
	 */
	public Geometry getResult()
	{
		return GeometryMapper.map(geom, new HoleRemoverMapOp());
	}

	private class HoleRemoverMapOp implements GeometryMapper.MapOp {
		public Geometry map(Geometry geom) {
			if (geom instanceof Polygon)
				return  PolygonHoleRemover.clean((Polygon) geom, isRemoved);
			return geom;
		}
	}

	private static class PolygonHoleRemover {

		public static Polygon clean(Polygon poly, Predicate isRemoved) {
			PolygonHoleRemover pihr = new PolygonHoleRemover(poly, isRemoved);
			return pihr.getResult();
		}

		private Polygon poly;
		private Predicate isRemoved;

		public PolygonHoleRemover(Polygon poly, Predicate isRemoved) {
			this.poly = poly;
			this.isRemoved = isRemoved;
		}

		public Polygon getResult()
		{
			GeometryFactory gf = poly.getFactory();
			Polygon shell = gf.createPolygon((LinearRing) poly.getExteriorRing());

			List holes = new ArrayList();
			for (int i = 0; i < poly.getNumInteriorRing(); i++) {
				LinearRing hole = (LinearRing) poly.getInteriorRingN(i);
				if (! isRemoved.value(hole)) {
					holes.add(hole);
				}
			}
			// all holes valid, so return original
			if (holes.size() == poly.getNumInteriorRing())
				return poly;

			// return new polygon with covered holes only
			Polygon result = gf.createPolygon((LinearRing) poly.getExteriorRing(),
					GeometryFactory.toLinearRingArray(holes));
			return result;
		}

	}
}
