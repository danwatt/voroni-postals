package org.danwatt.voronipostals.repository

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.index.SpatialIndex
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.io.WKTReader
import org.danwatt.voronipostals.component.GeoUtils
import org.danwatt.voronipostals.representation.PostalCode
import org.danwatt.voronipostals.service.VoroniComputer
import java.nio.charset.Charset

class PostalSource {

    val postalCodes: Map<String, PostalCode>
    val postalIndex: SpatialIndex
    val countyIndex: SpatialIndex

    private constructor(path: String, boundingFile: String?) {
        println("Loading postal codes")
        this.postalCodes = buildPostals(path)
        val usBoundary = loadBoundingFile(boundingFile)
        this.postalIndex = VoroniComputer(GeometryFactory()).computeVoroni(postalCodes.values, usBoundary)
        this.countyIndex = buildCounties()
        println("Loaded ${postalCodes.size} postal codes")
    }

    private fun loadBoundingFile(boundingFile: String?): Geometry? {
        boundingFile ?: return null
        return WKTReader().read(PostalSource::class.java.classLoader.getResource(boundingFile)
            .readText(Charset.forName("UTF-8"))
        )
    }

    private fun buildPostals(path: String): Map<String, PostalCode> =
        PostalSource::class.java.classLoader.getResource(path)
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
            if (null == it.wkt) {
                System.err.println("Null WKT found for $it")
            }
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
        fun load(file: String, boundingFile: String?): PostalSource = PostalSource(file, boundingFile)
    }

}
