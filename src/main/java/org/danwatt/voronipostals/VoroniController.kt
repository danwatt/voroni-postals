package org.danwatt.voronipostals

import org.rapidoid.annotation.Controller
import org.rapidoid.annotation.GET
import org.rapidoid.annotation.Page
import java.lang.Double.parseDouble
import java.util.Collections.singletonList

@Controller
class VoroniController(private val instance: PostalSource = PostalSource.instance) {

    @Page("/map")
    fun map(): Any {
        return ""
    }

    @GET("/nearby/counties/{point}")
    fun nearby(point: String): List<GeoContainer> {
        val parts = point.split(",".toRegex()).dropLastWhile(String::isEmpty)
        val lat = parseDouble(parts[0])
        val lon = parseDouble(parts[1])
        return PostalQueries.collectNearbyCounties(lat, lon, instance.countyIndex)
    }

    @GET("/nearby/postals/{point}")
    fun nearbyPostals(point: String): List<GeoContainer> {
        val parts = point.split(",".toRegex()).dropLastWhile(String::isEmpty)
        val lat = parseDouble(parts[0])
        val lon = parseDouble(parts[1])

        return PostalQueries.collectNearbyPostals(lat, lon, instance.postalIndex, instance.postalCodes)
    }

    @GET("/postals/union/{postals}")
    fun getUnion(postals: String): List<GeoContainer> {
        val splitPostals = postals.split(",".toRegex()).dropLastWhile(String::isEmpty)
        val union = PostalQueries.unionPostalCodes(splitPostals, instance.postalCodes) ?: return emptyList()
        return singletonList(union)

    }


}
