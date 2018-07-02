package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.index.SpatialIndex
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.io.WKTReader
import java.util.*

object GeoUtils {
    @JvmStatic
    val geometryFactory = GeometryFactory()
    @JvmStatic
    val reader = WKTReader(geometryFactory)
    val MAP_COLORS = Arrays.asList(
            "#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c",
            "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928")

    fun color(matches: List<Pair<Geometry, GeoContainer>>) {
        val index = STRtree(matches.size)
        matches.forEach { index.insert(it.first.envelopeInternal, it.first) }
        val colors = LinkedHashMap<String, String>()
        matches.forEach {
            val potentialColors = ArrayList(MAP_COLORS)
            val neighbors = getNeighbors(index, it.first)
            potentialColors.removeAll(neighbors.map { colors[it.userData] }.filter { it != null })
            colors[it.first.userData as String] = potentialColors[0]
            it.second.color = potentialColors[0]
        }
    }

    fun getNeighbors(index: SpatialIndex, geometry: Geometry): List<Geometry> {
        val matches: List<Geometry> = index.query(geometry.envelopeInternal) as List<Geometry>
        return matches.filter { it.intersects(geometry) && it !== geometry }
    }
}