package org.danwatt.voronipostals;

import org.locationtech.jts.algorithm.Area;
import org.locationtech.jts.geom.Geometry;

public class SmallHoleRemover {

	private static class IsSmall implements HoleRemover.Predicate {
		private double area;

		public IsSmall(double area) {
			this.area = area;
		}

		public boolean value(Geometry geom) {
			double holeArea = Area.ofRing(geom.getCoordinates());
			return holeArea <= area;
		}

	}

	/**
	 * Removes small holes from the polygons in a geometry.
	 *
	 * @param geom the geometry to clean
	 * @return the geometry with invalid holes removed
	 */
	public static Geometry clean(Geometry geom, double areaTolerance) {
		HoleRemover remover = new HoleRemover(geom, new IsSmall(areaTolerance));
		return remover.getResult();
	}

}