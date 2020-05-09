package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.index.strtree.STRtree

object PostalQueries {

    fun collectNearbyCounties(lat: Double, lon: Double, countyIndex: STRtree): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(countyIndex, point.buffer(2.0))
        val matches = results.map { geo -> Pair(geo, createCounty(geo)) }
        GeoUtils.color(matches)
        return matches.map { it.second }
    }

    private fun createCounty(geo: Geometry): County {
        val userData = geo.userData as String
        val state = userData.substringAfterLast(",")
        val county = userData.substringBeforeLast(",")
        return County(state, county).apply {
            wkt = geo.toText()
        }
    }

    fun collectNearbyPostals(
        lat: Double,
        lon: Double,
        postalIndex: STRtree,
        postalCodes: Map<String, PostalCode>
    ): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(postalIndex, point.buffer(0.5))

        val matches = results.map { Pair(it, postalCodes[it.userData]!!) }
        //TODO: Colorization only needs to happen at init time
        GeoUtils.color(matches)

        return matches.map { it.second }
    }

    fun unionPostalCodes(strings: List<String>, postalCodes: Map<String, PostalCode>): PostalCode? {
        val union = strings.stream()
            .map<Geometry> { parse(postalCodes[it]!!.wkt) }
            .filter { it != null }
            .reduce { obj, other -> obj.union(other) }
        return when {
            union.isPresent -> union.map { createPostalFromGeo(it) }.orElse(null)
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
