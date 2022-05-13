package org.danwatt.voronipostals.service

import com.vividsolutions.jts.geom.Coordinate
import org.danwatt.voronipostals.component.GeoUtils
import org.danwatt.voronipostals.repository.PostalSource
import org.danwatt.voronipostals.representation.GeoContainer
import org.springframework.stereotype.Service

@Service
class PostalService(val postalSource: PostalSource) {
    fun collectNearbyPostals(
        lat: Double,
        lon: Double
    ): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(postalSource.postalIndex, point.buffer(0.5))
        if (results.isEmpty()) return emptyList()

        val matches = results.associateWith { postalSource.postalCodes[it.userData]!! }
        GeoUtils.color(matches)
        return matches.values.toList()
    }

    /*fun unionPostalCodes(strings: List<String>, postalCodes: Map<String, PostalCode>): PostalCode? {
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

     */
}