package org.danwatt.voronipostals.repository

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.index.strtree.STRtree
import org.danwatt.voronipostals.component.GeoUtils
import org.danwatt.voronipostals.representation.PostalCode
import org.danwatt.voronipostals.service.VoroniComputer
import java.nio.charset.Charset

class PostalSource {

    val postalCodes: Map<String, PostalCode>
    val postalIndex: STRtree
    val countyIndex: STRtree

    private constructor(path: String) {
        println("Loading postal codes")
        this.postalCodes = buildPostals(path)
        this.postalIndex = VoroniComputer(GeometryFactory()).computeVoroni(postalCodes.values)
        this.countyIndex = buildCounties()
        println("Loaded ${postalCodes.size} postal codes")
    }

    private fun buildPostals(path: String): Map<String, PostalCode> =
        GeoUtils::class.java.classLoader.getResource(path)
            .readText(Charset.forName("UTF-8"))
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { PostalCode.loadLine(it) }
            .distinct()
            .map { it.postal to it }
            .toMap()


    private fun buildCounties(): STRtree {
        val countyIndex = STRtree()
        val countyNameToPolygon: Map<String, Geometry> = postalCodes.values.map {
            val geometry = GeoUtils.reader.read(it.wkt!!)
            val countyState = "${it.county}, ${it.state}"
            geometry.userData = countyState
            countyState to geometry
        }
            .groupBy { it.first }
            .map { (name, postalCodes) ->
                name to postalCodes
                    .map { it.second }
                    .reduce(::mergeGeometries)
            }.toMap()

        countyNameToPolygon.forEach { (_, v) -> countyIndex.insert(v.envelopeInternal, v) }
        return countyIndex
    }

    private fun mergeGeometries(acc: Geometry, geometry: Geometry): Geometry =
        acc.union(geometry)
            .also { it.userData = acc.userData }

    companion object {
        fun load(file: String): PostalSource = PostalSource(file)
    }

}
