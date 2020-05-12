package org.danwatt.voronipostals.controller

import org.danwatt.voronipostals.representation.GeoResults
import org.danwatt.voronipostals.service.PostalQueries
import org.danwatt.voronipostals.repository.PostalSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.lang.Double.parseDouble
import java.util.Collections.singletonList

@RestController
class VoroniController(
    private val postalSource: PostalSource
) {

    @GetMapping("/nearby/counties/{point}")
    fun counties(@PathVariable("point") point: String): ResponseEntity<GeoResults> {
        val (lat, lon) = parseLatLon(point) ?: return ResponseEntity.badRequest().build()
        val counties = PostalQueries.collectNearbyCounties(
            lat,
            lon,
            postalSource.countyIndex
        )
        return ResponseEntity.ok(GeoResults(counties))
    }

    @GetMapping("/nearby/postals/{point}")
    fun nearbyPostals(@PathVariable("point") point: String): ResponseEntity<GeoResults> {
        val (lat, lon) = parseLatLon(point) ?: return ResponseEntity.badRequest().build()
        val postals = PostalQueries.collectNearbyPostals(
            lat,
            lon,
            postalSource.postalIndex,
            postalSource.postalCodes
        )
        return ResponseEntity.ok(GeoResults(postals))
    }

    @GetMapping("/postals/union/{postals}")
    fun getUnion(@PathVariable("postals") postals: String): GeoResults {
        val splitPostals = postals.split(",".toRegex()).dropLastWhile(String::isEmpty)
        val union = PostalQueries.unionPostalCodes(
            splitPostals,
            postalSource.postalCodes
        ) ?: return GeoResults(emptyList())
        return GeoResults(singletonList(union))
    }

}

typealias Point = Pair<Double, Double>

private val commaSplitter = ",".toRegex()
private fun parseLatLon(point: String): Point? {
    val parts = point.split(commaSplitter).dropLastWhile(String::isEmpty).map { it.toDoubleOrNull() }
    if (parts.count { it != null } != 2) return null
    val (lat, lon) = parts.filterNotNull()
    return Point(lat, lon)
}
