package org.danwatt.voronipostals.component

import com.vividsolutions.jts.geom.*
import com.vividsolutions.jts.geom.util.GeometryMapper

import java.util.ArrayList

object HoleRemover {

    @JvmStatic
    fun cleanSmallHoles(geom: Geometry, areaTolerance: Double): Geometry =
        GeometryMapper.map(geom, HoleRemoverMapOp(areaTolerance))
}

class HoleRemoverMapOp(private val areaTolerance: Double) : GeometryMapper.MapOp {
    override fun map(geom: Geometry): Geometry {
        if (geom !is Polygon) return geom

        val holesToKeep = ArrayList<LinearRing>()
        val numRings = geom.numInteriorRing
        for (i in 0 until numRings) {
            val hole = geom.getInteriorRingN(i) as LinearRing
            if (hole.area > areaTolerance) {
                holesToKeep.add(hole)
            }
        }

        return when (holesToKeep.size) {
            geom.numInteriorRing -> geom
            else -> geom.factory.createPolygon(
                geom.exteriorRing as LinearRing,
                GeometryFactory.toLinearRingArray(holesToKeep)
            )
        }
    }

}