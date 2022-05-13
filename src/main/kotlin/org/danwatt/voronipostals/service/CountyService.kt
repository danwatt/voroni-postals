package org.danwatt.voronipostals.service

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import org.danwatt.voronipostals.component.GeoUtils
import org.danwatt.voronipostals.repository.PostalSource
import org.danwatt.voronipostals.representation.County
import org.danwatt.voronipostals.representation.GeoContainer
import org.springframework.stereotype.Service

@Service
class CountyService(val postalSource: PostalSource) {

    fun collectNearbyCounties(lat: Double, lon: Double): List<GeoContainer> {
        val point = GeoUtils.geometryFactory.createPoint(Coordinate(lon, lat))
        val results = GeoUtils.getNeighbors(postalSource.countyIndex, point.buffer(2.0))
        val matches = results.associateWith { createCounty(it) }
        GeoUtils.color(matches)
        return matches.values.toList()
    }

    private fun createCounty(geo: Geometry): County {
        val userData = geo.userData as String
        val state = userData.substringAfterLast(",")
        val county = userData.substringBeforeLast(",")
        return County(state.trim(), county.trim()).apply { wkt = geo.toText() }
    }
}