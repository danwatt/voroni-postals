package org.danwatt.voronipostals.service

import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.PrecisionModel
import com.vividsolutions.jts.index.strtree.STRtree
import com.vividsolutions.jts.io.WKTReader
import org.assertj.core.api.Assertions.assertThat
import org.danwatt.voronipostals.representation.PostalCode
import org.junit.Test

class VoroniComputerTests {

    private val mauryCountyTnBoundingBox =
        WKTReader().read("POLYGON((-87.34 35.85,-86.60 35.85,-86.60 35.32,-87.34 35.32,-87.34 35.85))")

    private val geometryFactory = GeometryFactory(PrecisionModel(40.0))

    @Test
    fun test() {
        val mauryTn = listOf(
            PostalCode(postal = "38401", latitude = 35.6156, longitude = -87.038),
            PostalCode(postal = "38402", latitude = 35.6294, longitude = -87.0682),
            PostalCode(postal = "38451", latitude = 35.4749, longitude = -87.0005),
            PostalCode(postal = "38461", latitude = 35.5915, longitude = -87.3251),
            PostalCode(postal = "38474", latitude = 35.5301, longitude = -87.2037),
            PostalCode(postal = "38482", latitude = 35.7588, longitude = -87.1515),
            PostalCode(postal = "37174", latitude = 35.7173, longitude = -86.9048),
            PostalCode(postal = "38487", latitude = 35.6494, longitude = -87.2257)
        )

        val spatialIndex = VoroniComputer(geometryFactory)
            .computeVoroni(mauryTn, mauryCountyTnBoundingBox)
        (spatialIndex as STRtree).itemsTree().forEach { println(it) }

        assertScenario(spatialIndex, 35.7 to -86.9, "37174")
        assertScenario(spatialIndex, 35.6 to -87.0, "38401")
        assertScenario(spatialIndex, 36.16 to -86.78, null)
    }

    private fun assertScenario(
        spatialIndex: STRtree,
        latLon: Pair<Double, Double>,
        expectedPostal: String?
    ) {
        val (lat, lon) = latLon
        val point = geometryFactory.createPoint(Coordinate(lon, lat))
        val results = spatialIndex.query(point.envelopeInternal)
        if (expectedPostal == null) {
            assertThat(results).isEmpty()
        } else {
            assertThat(results).hasSizeGreaterThanOrEqualTo(1)
            assertThat((results[0] as Geometry).userData).isEqualTo(expectedPostal)
        }
    }
}