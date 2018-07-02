package org.danwatt.voronipostals

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryCollection
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder
import org.apache.commons.lang3.time.StopWatch

object VoroniComputer {
    fun computeVoroni(
            postalCodes: Collection<PostalCode>,
            postalIndex: STRtree
    ) {
        val sw =  StopWatch();
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
        postalToCenter.forEach {
            val point = GeoUtils.geometryFactory.createPoint(it.second)
            val query = spatialIndex.query(point.envelopeInternal) as List<Geometry>
            val match = query.firstOrNull { point.intersects(it) }
            if (match != null) {
                it.first.wkt = match.toText()
                match.userData = it.first.postal
                postalIndex.insert(match.envelopeInternal, match)
            }
        }
    }
}
