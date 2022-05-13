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
        val postalToCenter = postalCodes.associateWith { Coordinate(it.longitude, it.latitude) }
        val diagram = buildVoroniDiagram(postalToCenter, boundingArea) ?: return STRtree()
        val spatialIndex = buildInitialSpatialIndex(diagram)
        return placePointsIntoIndex(postalToCenter, spatialIndex)
    }

    private fun placePointsIntoIndex(
        postalToCenter: Map<PostalCode, Coordinate>,
        spatialIndex: STRtree
    ): STRtree {
        val postalIndex = STRtree()
        postalToCenter.forEach { (postalCode, coordinate) ->
            val point = geometryFactory.createPoint(coordinate)
            val query = spatialIndex.query(point.envelopeInternal) as List<Geometry>
            val match = query.firstOrNull { point.intersects(it) }
            if (match == null) {
                System.err.println("A match could not be found for $point. ${query.size} were found nearby")
            } else {
                postalCode.wkt = match.toText()
                match.userData = postalCode.postal
                postalIndex.insert(match.envelopeInternal, match)
            }
        }
        return postalIndex
    }

    private fun buildInitialSpatialIndex(gc: Geometry): STRtree {
        val spatialIndex = STRtree()
        (0 until gc.numGeometries)
            .map { gc.getGeometryN(it) }
                /*
                TODO: Utilize GeometrySnapper
                 */
            .forEach { spatialIndex.insert(it.envelopeInternal, it) }
        spatialIndex.build()
        return spatialIndex
    }

    private fun buildVoroniDiagram(
        postalToCenter: Map<PostalCode, Coordinate>,
        boundingArea: Geometry?
    ): Geometry? {
        val diagramBuilder = VoronoiDiagramBuilder()

        diagramBuilder.setSites(postalToCenter.values)
        if (null != boundingArea) {
            diagramBuilder.setClipEnvelope(boundingArea.envelopeInternal)
        }
        /* if (boundingArea != null) {
            return diagram.intersection(boundingArea)
        }*/
        return diagramBuilder.getDiagram(geometryFactory)
    }
}
