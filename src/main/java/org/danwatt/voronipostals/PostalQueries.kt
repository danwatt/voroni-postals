package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.index.strtree.STRtree
import org.apache.commons.lang3.StringUtils
import java.util.*

object PostalQueries {

    fun collectNearbyCounties(lat: Double, lon: Double, countyIndex: STRtree): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(countyIndex, point.buffer(2.0))

        val matches = results.map { geo -> Pair(geo, createCounty(geo)) }
        GeoUtils.color(matches)
        return matches.map { it.second }
    }

    private fun createCounty(geo: Geometry): County {
        val state = StringUtils.substringAfterLast(geo.userData as String, ",")
        val county = StringUtils.substringBeforeLast(geo.userData as String, ",")
        val c = County(state, county)
        c.wkt = geo.toText()
        return c
    }

    fun collectNearbyPostals(lat: Double, lon: Double, postalIndex: STRtree, postalCodes: Map<String, PostalCode>): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(postalIndex, point.buffer(0.5))

        val matches = results.map { Pair(it, postalCodes[it.userData]!!) }
        //TODO: Colorization only needs to happen at init time
        GeoUtils.color(matches)

        return matches.map { it.second }
    }

    fun unionPostalCodes(strings: List<String>, postalCodes: Map<String, PostalCode>): Optional<PostalCode> {
        val union = strings.stream()
                .map<Geometry> { postal ->
                    val wkt = postalCodes[postal]!!.wkt
                    parse(wkt)
                }
                .filter { it != null }
                .reduce({ obj, other -> obj.union(other) })
        return if (union.isPresent) {
            union.map{ createPostalFromGeo(it) }
        } else {
            Optional.empty()
        }
    }

    private fun parse(wkt: String?): Geometry? {
        try {
            return GeoUtils.reader.read(wkt!!)
        } catch (ex: Exception) {
            return null
        }

    }

    private fun createPostalFromGeo(geo: Geometry): PostalCode {
        val pc = PostalCode()
        pc.wkt = geo.toText()
        pc.color = GeoUtils.MAP_COLORS[1]
        return pc
    }
}
