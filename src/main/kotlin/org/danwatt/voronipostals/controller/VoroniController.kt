package org.danwatt.voronipostals.controller

import org.danwatt.voronipostals.repository.PostalSource
import org.danwatt.voronipostals.representation.GeoResults
import org.danwatt.voronipostals.service.CountyService
import org.danwatt.voronipostals.service.PostalQueries
import org.danwatt.voronipostals.service.PostalService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class VoroniController(
    val countyService: CountyService,
    val postalService: PostalService
) {
    @GetMapping("/nearby/counties/{point}")
    fun counties(@PathVariable("point") point: String): ResponseEntity<GeoResults> {
        val (lat, lon) = parseLatLon(point) ?: return ResponseEntity.badRequest().build()
        val counties = countyService.collectNearbyCounties(lat, lon)
        return ResponseEntity.ok(GeoResults(counties))
    }

    @GetMapping("/nearby/postals/{point}")
    fun nearbyPostals(@PathVariable("point") point: String): ResponseEntity<GeoResults> {
        val (lat, lon) = parseLatLon(point) ?: return ResponseEntity.badRequest().build()
        val postals = postalService.collectNearbyPostals(lat, lon)
        return ResponseEntity.ok(GeoResults(postals))
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
