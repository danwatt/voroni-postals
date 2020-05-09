package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryCollection
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder

object VoroniComputer {
    fun computeVoroni(
        postalCodes: Collection<PostalCode>,
        postalIndex: STRtree
    ) {
        val diagramBuilder = VoronoiDiagramBuilder()
        val postalToCenter = postalCodes.map { Pair(it, Coordinate(it.longitude, it.latitude)) }
        diagramBuilder.setSites(postalToCenter.map { it.second })
        val diagram = diagramBuilder.getDiagram(GeoUtils.geometryFactory)
        val gc = diagram as GeometryCollection
        val spatialIndex = STRtree()
        for (i in 0 until gc.numGeometries) {
            val geometryN = gc.getGeometryN(i)
            spatialIndex.insert(geometryN.envelopeInternal, geometryN)
        }
        postalToCenter.forEach { p ->
            val point = GeoUtils.geometryFactory.createPoint(p.second)
            val query = spatialIndex.query(point.envelopeInternal) as List<Geometry>
            val match = query.firstOrNull { point.intersects(it) }
            if (match != null) {
                p.first.wkt = match.toText()
                match.userData = p.first.postal
                postalIndex.insert(match.envelopeInternal, match)
            }
        }
    }
}
