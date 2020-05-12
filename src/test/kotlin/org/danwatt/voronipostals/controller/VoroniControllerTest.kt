package org.danwatt.voronipostals.controller

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.danwatt.voronipostals.representation.County
import org.danwatt.voronipostals.representation.PostalCode
import org.danwatt.voronipostals.service.CountyService
import org.danwatt.voronipostals.service.PostalService
import org.junit.Before
import org.junit.Test

class VoroniControllerTest {

    @MockK
    lateinit var postalService: PostalService

    @MockK
    lateinit var countyService: CountyService

    @InjectMockKs
    lateinit var voroniController: VoroniController

    @Before
    fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

    @Test
    fun `valid postal request`() {
        val mockedCodes = listOf(PostalCode(postal = "12345"))
        every { postalService.collectNearbyPostals(1.0, 2.0) }.returns(mockedCodes)

        val result = voroniController.nearbyPostals("1,2")
        assertThat(result.statusCodeValue).isEqualTo(200)
        assertThat(result.body.results).isSameAs(mockedCodes)
    }

    @Test
    fun `valid county request`() {
        val mockedCounties = listOf(County(state = "TN", county = "Maury"))
        every { countyService.collectNearbyCounties(1.0, 2.0) }.returns(mockedCounties)

        val result = voroniController.counties("1,2")
        assertThat(result.statusCodeValue).isEqualTo(200)
        assertThat(result.body.results).isSameAs(mockedCounties)
    }

    @Test
    fun `bad postal request`() {

        val result = voroniController.nearbyPostals("1")
        assertThat(result.statusCodeValue).isEqualTo(400)

        verify(exactly = 0) { postalService.collectNearbyPostals(any(), any()) }
    }

    @Test
    fun `bad county request`() {

        val result = voroniController.counties("1")
        assertThat(result.statusCodeValue).isEqualTo(400)

        verify(exactly = 0) { countyService.collectNearbyCounties(any(), any()) }
    }

}