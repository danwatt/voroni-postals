package org.danwatt.voronipostals

import org.assertj.core.api.Assertions.*
import org.junit.Test

class IntegrationTests {
    companion object {
        val postalSource = PostalSource.instance
    }

    @Test
    fun postals() {
        assertThat(postalSource.postalCodes).hasSize(40937)
        assertThat(postalSource.postalCodes["37174"]!!.wkt).isEqualTo(
                "POLYGON ((-87.02651939257072 35.747743273561504, " +
                        "-87.02088560603968 35.781233758795366, " +
                        "-86.82826441096637 35.752876221898, " +
                        "-86.79494147956763 35.674347371424176, " +
                        "-86.82323538120409 35.59792965418384, " +
                        "-86.88375804739363 35.568901320691296, " +
                        "-86.9025998856794 35.576340115757084, " +
                        "-87.0099469825848 35.7169363134739, " +
                        "-87.02651939257072 35.747743273561504))")
        val nearby = PostalQueries.collectNearbyPostals(35.74205383068037, -86.92005157470703,
                postalSource.postalIndex, postalSource.postalCodes)

        assertThat((nearby[0] as PostalCode).postal).isEqualTo("37033")
        assertThat(nearby[0].wkt).isEqualTo(
                "POLYGON ((-87.64154073797337 35.66040243998613, " +
                        "-87.64265440091671 35.67213302298933, " +
                        "-87.58232460924867 35.80072713674886, " +
                        "-87.41115040165133 35.85246923676791, " +
                        "-87.40064729935348 35.8279173040206, " +
                        "-87.44732831373354 35.65150135693129, " +
                        "-87.64154073797337 35.66040243998613))")
    }

    @Test
    fun county() {
        assertThat(postalSource.countyIndex.size()).isEqualTo(3149)
    }

    @Test
    fun usBorderBuilder() {
        val boundary = UsBorderBuilder.buildSimplifiedUsBoundary()
        println(boundary.toText())
        assertThat(boundary.numGeometries).isEqualTo(49)
        assertThat(boundary.numPoints).isEqualTo(2627)
    }
}