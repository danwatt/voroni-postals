package org.danwatt.voronipostals.service

import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.io.WKTReader
import org.danwatt.voronipostals.representation.PostalCode
import org.junit.Test

class VoroniComputerTests {

    private val mauryCountyTnBoundingBox =
        WKTReader().read("POLYGON((-87.34 35.85,-86.60 35.85,-86.60 35.32,-87.34 35.32,-87.34 35.85))")

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
        val strTree = VoroniComputer(GeometryFactory()).computeVoroni(mauryTn, mauryCountyTnBoundingBox)
        strTree.itemsTree().forEach { println(it) }

        val springHillPoint = 35.738654 to -86.946806
        val columbiaSquare = 35.614833 to -87.033800
        val tnCapitol = 36.165855 to -86.784501
    }
}