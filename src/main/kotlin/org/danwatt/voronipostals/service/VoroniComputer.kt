package org.danwatt.voronipostals.service

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryCollection
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder
import org.danwatt.voronipostals.representation.PostalCode
import org.springframework.stereotype.Service

@Service
class VoroniComputer(val geometryFactory: GeometryFactory) {
    fun computeVoroni(
        postalCodes: Collection<PostalCode>,
        boundingArea: Geometry? = null
    ): STRtree {
        val postalIndex = STRtree()
        val diagramBuilder = VoronoiDiagramBuilder()
        val postalToCenter = postalCodes.map { it to Coordinate(it.longitude, it.latitude) }
        diagramBuilder.setSites(postalToCenter.map { it.second })
        var diagram = diagramBuilder.getDiagram(geometryFactory)
        if (boundingArea != null) {
            diagram = diagram.intersection(boundingArea)
        }
        val gc = diagram as GeometryCollection
        val spatialIndex = STRtree()
        (0 until gc.numGeometries)
            .map { gc.getGeometryN(it) }
            .forEach { spatialIndex.insert(it.envelopeInternal, it) }
        postalToCenter.forEach { (postalCode, coordinate) ->
            val point = geometryFactory.createPoint(coordinate)
            val query = spatialIndex.query(point.envelopeInternal) as List<Geometry>
            val match = query.firstOrNull { point.intersects(it) }
            if (match != null) {
                postalCode.wkt = match.toText()
                match.userData = postalCode.postal
                postalIndex.insert(match.envelopeInternal, match)
            }
        }
        return postalIndex
    }
}
