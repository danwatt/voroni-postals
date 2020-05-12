package org.danwatt.voronipostals.service

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.index.SpatialIndex
import com.vividsolutions.jts.index.strtree.STRtree
import org.danwatt.voronipostals.component.GeoUtils
import org.danwatt.voronipostals.representation.County
import org.danwatt.voronipostals.representation.GeoContainer
import org.danwatt.voronipostals.representation.PostalCode

object PostalQueries {

    fun collectNearbyCounties(lat: Double, lon: Double, countyIndex: SpatialIndex): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(countyIndex, point.buffer(2.0))
        val matches = results.map { it to createCounty(it) }.toMap()
        GeoUtils.color(matches)
        return matches.values.toList()
    }

    private fun createCounty(geo: Geometry): County {
        val userData = geo.userData as String
        val state = userData.substringAfterLast(",")
        val county = userData.substringBeforeLast(",")
        return County(state.trim(), county.trim()).apply { wkt = geo.toText() }
    }

    fun collectNearbyPostals(
        lat: Double,
        lon: Double,
        postalIndex: SpatialIndex,
        postalCodes: Map<String, PostalCode>
    ): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(postalIndex, point.buffer(0.5))
        if (results.isEmpty()) return emptyList()

        val matches = results.map { it to postalCodes[it.userData]!! }.toMap()
        GeoUtils.color(matches)
        return matches.values.toList()
    }

    fun unionPostalCodes(strings: List<String>, postalCodes: Map<String, PostalCode>): PostalCode? {
        val union = strings.stream()
            .map<Geometry> { parse(postalCodes[it]!!.wkt) }
            .filter { it != null }
            .reduce { obj, other -> obj.union(other) }
        return when {
            union.isPresent -> union.map {
                createPostalFromGeo(it)
            }.orElse(null)
            else -> null
        }
    }

    private fun parse(wkt: String?): Geometry? = try {
        GeoUtils.reader.read(wkt!!)
    } catch (ex: Exception) {
        null
    }

    private fun createPostalFromGeo(geo: Geometry): PostalCode =
        PostalCode().apply {
            wkt = geo.toText()
            color = GeoUtils.MAP_COLORS[1]
        }
}
