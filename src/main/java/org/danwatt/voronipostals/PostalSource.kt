package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.index.strtree.STRtree

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors

class PostalSource private constructor() {

    var postalCodes: Map<String, PostalCode>
    var postalIndex = STRtree()
    var countyIndex = STRtree()

    init {
        try {

            val path: Path = GeoUtils.getPath("US.txt")
            postalCodes = Files.lines(path)
                    .map { PostalCode.loadLine(it) }
                    .distinct()
                    .collect(Collectors.toMap(PostalCode::postal, { it }))
            VoroniComputer.computeVoroni(postalCodes.values, postalIndex)
            buildCounties()
        } catch (ex: Exception) {
            ex.printStackTrace()
            postalCodes = HashMap()
        }

    }


    private fun buildCounties() {
        val countyNameToPolygon: Map<String, Geometry> = postalCodes.values.map {
            val geometry = GeoUtils.reader.read(it.wkt!!)
            geometry.userData = it.county + ", " + it.state
            geometry.userData as String to geometry
        }
                .groupBy { it.first }
                .map { (name, postalCodes) ->
                    name to postalCodes
                            .map { it.second }
                            .reduce(::mergeGeometries)
                }.toMap();

        countyNameToPolygon.forEach { (_, v) -> countyIndex.insert(v.envelopeInternal, v) }
    }

    private fun mergeGeometries(acc: Geometry, geometry: Geometry): Geometry {
        val union = acc.union(geometry)
        union.userData = acc.userData
        return union
    }

    companion object {
        val instance: PostalSource

        init {
            println("Loading postal codes")
            val ps = PostalSource()
            println("Loaded ${ps.postalCodes.size} postal codes")
            instance = ps
        }
    }

}
