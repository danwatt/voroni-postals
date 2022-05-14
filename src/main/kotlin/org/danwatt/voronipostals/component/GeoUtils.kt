package org.danwatt.voronipostals.component

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.index.SpatialIndex
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.geom.PrecisionModel
import org.danwatt.voronipostals.representation.GeoContainer

object GeoUtils {
    @JvmStatic
    val geometryFactory = GeometryFactory()

    @JvmStatic
    val lowPrecisionFactory = GeometryFactory(PrecisionModel(40.0))

    @JvmStatic
    val reader = WKTReader(geometryFactory)
    private val MAP_COLORS = listOf(
        "#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c",
        "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928"
    )

    fun color(matches: Map<Geometry, GeoContainer>) {
        val index = STRtree(matches.size)
        matches.forEach { (g, _) -> index.insert(g.envelopeInternal, g) }
        val colors = LinkedHashMap<String, String>()
        matches.forEach { (g, gc) ->
            val potentialColors = ArrayList(MAP_COLORS)
            val neighbors = getNeighbors(index, g)
            potentialColors.removeAll(neighbors.mapNotNull { colors[it.userData] })
            colors[g.userData as String] = potentialColors[0]
            gc.color = potentialColors[0]
        }
    }

    fun getNeighbors(index: SpatialIndex, geometry: Geometry): List<Geometry> =
        (index.query(geometry.envelopeInternal) as List<Geometry>)
            .filter { it !== geometry }
            .filter { it.intersects(geometry) }

}