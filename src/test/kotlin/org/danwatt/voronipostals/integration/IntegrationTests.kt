package org.danwatt.voronipostals.integration

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc(print = MockMvcPrint.SYSTEM_OUT, printOnlyOnFailure = false)
class IntegrationTests : BaseTests() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `get nearby postal codes`() {
        val honoluluLatLon = "21.35,-157.91"
        mockMvc.get("/nearby/postals/{point}", honoluluLatLon) {
        }.andExpect {
            status { isOk }
            jsonPath("$.results") { isArray }
            jsonPath("$.results[0].postal") { value("96707") }
            jsonPath("$.results[0].city") { value("Kapolei") }
        }
    }

    @Test
    fun `get nearby counties`() {
        val honoluluLatLon = "21.35,-157.91"
        mockMvc.get("/nearby/counties/{point}", honoluluLatLon) {
        }.andExpect {
            status { isOk }
            jsonPath("$.results") { isArray }
            jsonPath("$.results[0].county") { value("Hawaii") }
            jsonPath("$.results[0].state") { value("Hawaii") }
        }
    }
/*
    @Test
    fun `no counties near null island`() {
        val nullIsland = "0,0"
        mockMvc.get("/nearby/counties/{point}", nullIsland) {
        }.andExpect {
            status { isNotFound }
        }
    }

    @Test
    fun `no postals near null island`() {
        val nullIsland = "0,0"
        mockMvc.get("/nearby/postals/{point}", nullIsland) {
        }.andExpect {
            status { isNotFound }
        }
    }

 */
}