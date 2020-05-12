package org.danwatt.voronipostals.controller

import org.danwatt.voronipostals.representation.GeoResults
import org.danwatt.voronipostals.service.PostalQueries
import org.danwatt.voronipostals.repository.PostalSource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.lang.Double.parseDouble
import java.util.Collections.singletonList

@RestController
class VoroniController(
    private val postalSource: PostalSource
) {
    private val commaSplitter = ",".toRegex()

    @GetMapping("/nearby/counties/{point}")
    fun counties(@PathVariable("point") point: String): GeoResults {
        val parts = point.split(commaSplitter).dropLastWhile(String::isEmpty)
        val lat = parseDouble(parts[0])
        val lon = parseDouble(parts[1])
        return GeoResults(
            PostalQueries.collectNearbyCounties(
                lat,
                lon,
                postalSource.countyIndex
            )
        )
    }

    @GetMapping("/nearby/postals/{point}")
    fun nearbyPostals(@PathVariable("point") point: String): GeoResults {
        val parts = point.split(commaSplitter).dropLastWhile(String::isEmpty)
        val lat = parseDouble(parts[0])
        val lon = parseDouble(parts[1])

        return GeoResults(
            PostalQueries.collectNearbyPostals(
                lat,
                lon,
                postalSource.postalIndex,
                postalSource.postalCodes
            )
        )
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
