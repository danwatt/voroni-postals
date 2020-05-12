package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.LinearRing
import com.vividsolutions.jts.geom.Polygon
import com.vividsolutions.jts.geom.util.GeometryMapper
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier
import org.apache.commons.io.FileUtils
import org.danwatt.voronipostals.component.GeoUtils
import java.io.File
import java.nio.charset.Charset

/**
 * Simplify the US WKT file, which has about 6500 points, to one with about 2600 points
 * This also smooths out costal regions, especially in Alaska, Louisiana, and other places.
 *
 * This is done by expanding the US border by a few miles, then by subtracting Canada and Mexico.
 * It also removes any interior holes. A good example of this is Galveston Bay near Houston.
 *
 * The resulting geometry will extend several miles out into the ocean, but will not extend into
 * neighboring countries.
 */
class UsBorderBuilder {

    companion object {
        private const val US_BORDER_EXPANSION = 0.06
        private const val US_BORDER_SIMPLIFICATION = 0.025
        private const val HOLE_TOLERANCE = 0.05

        fun buildSimplifiedUsBoundary(): Geometry {
            val us = loadFile("us.wkt")
            val canada = loadFile("can.wkt")
            val mexico = loadFile("mex.wkt")

            println("Original US file has ${us.numPoints} points and ${us.numGeometries}")

            val simplifiedUS = DouglasPeuckerSimplifier.simplify(
                us.buffer(US_BORDER_EXPANSION),
                US_BORDER_SIMPLIFICATION
            )

            val usAfterCut = simplifiedUS - canada - mexico

            var usGeom: Geometry = GeoUtils.lowPrecisionFactory.createGeometry(usAfterCut)!!
            usGeom = cleanSmallHoles(usGeom, HOLE_TOLERANCE)
            return usGeom.buffer(0.0).union()
        }

        private fun loadFile(file: String): Geometry =
            UsBorderBuilder::class.java.classLoader.getResource(file)
                .readText()
                .let { GeoUtils.reader.read(it) }
    }
}

private operator fun Geometry.minus(other: Geometry): Geometry = this.difference(other)

fun cleanSmallHoles(geom: Geometry, areaTolerance: Double): Geometry =
    GeometryMapper.map(geom, HoleRemoverMapOp(areaTolerance))

class HoleRemoverMapOp(private val areaTolerance: Double) : GeometryMapper.MapOp {
    override fun map(geom: Geometry): Geometry {
        if (geom !is Polygon) return geom

        val holesToKeep = (0 until geom.numInteriorRing)
            .map { geom.getInteriorRingN(it) as LinearRing }
            .filter { it.area > areaTolerance }

        return when (holesToKeep.size) {
            geom.numInteriorRing -> geom
            else -> geom.factory.createPolygon(
                geom.exteriorRing as LinearRing,
                GeometryFactory.toLinearRingArray(holesToKeep)
            )
        }
    }
}

fun main() {
    val less =
        UsBorderBuilder.buildSimplifiedUsBoundary()
    println("Generated a simplified US boundar with ${less.numPoints} points and ${less.numGeometries} geometries")
    FileUtils.writeStringToFile(File("/tmp/out.wk"), less.toText(), Charset.forName("UTF-8"))
}