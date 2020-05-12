package org.danwatt.voronipostals.service

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.index.SpatialIndex
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder
import org.danwatt.voronipostals.representation.PostalCode
import org.springframework.stereotype.Service

@Service
class VoroniComputer(val geometryFactory: GeometryFactory) {
    fun computeVoroni(
        postalCodes: Collection<PostalCode>,
        boundingArea: Geometry? = null
    ): SpatialIndex {
        val postalToCenter = postalCodes.map { it to Coordinate(it.longitude, it.latitude) }
        var diagram = buildVoroniDiagram(postalToCenter, boundingArea) ?: return STRtree()
        val spatialIndex = buildInitialSpatialIndex(diagram)
        return placePointsIntoIndex(postalToCenter, spatialIndex)
    }

    private fun placePointsIntoIndex(
        postalToCenter: List<Pair<PostalCode, Coordinate>>,
        spatialIndex: STRtree
    ): STRtree {
        val postalIndex = STRtree()
        postalToCenter.forEach { (postalCode, coordinate) ->
            val point = geometryFactory.createPoint(coordinate)
            val query = spatialIndex.query(point.envelopeInternal) as List<Geometry>
            val match = query.firstOrNull { point.intersects(it) }
            if (match != null) {
                postalCode.wkt = match.toText()
                match.userData = postalCode.postal
                postalIndex.insert(match.envelopeInternal, match)
            } else {
                System.err.println("Weird. A match could not be found for $point")
            }
        }
        return postalIndex
    }

    private fun buildInitialSpatialIndex(gc: Geometry): STRtree {
        val spatialIndex = STRtree()
        (0 until gc.numGeometries)
            .map { gc.getGeometryN(it) }
            .forEach { spatialIndex.insert(it.envelopeInternal, it) }
        return spatialIndex
    }

    private fun buildVoroniDiagram(
        postalToCenter: List<Pair<PostalCode, Coordinate>>,
        boundingArea: Geometry?
    ): Geometry? {
        val diagramBuilder = VoronoiDiagramBuilder()
        diagramBuilder.setSites(postalToCenter.map { it.second })
        var diagram = diagramBuilder.getDiagram(geometryFactory)
        if (boundingArea != null) {
            diagram = diagram.intersection(boundingArea)
        }
        return diagram
    }
}
