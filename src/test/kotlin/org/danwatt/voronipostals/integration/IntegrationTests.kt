package org.danwatt.voronipostals.integration

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc(print = MockMvcPrint.SYSTEM_OUT, printOnlyOnFailure = false)
class IntegrationTests @Autowired constructor(
    private val mockMvc: MockMvc
) : BaseTests() {

    val honoluluLatLon = "21.35,-157.91"

    @Test
    fun `get nearby postal codes`() {
        mockMvc.get("/nearby/postals/{point}", honoluluLatLon) {
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.results") { isArray() }
                jsonPath("$.results[0].postal") { value("96707") }
                jsonPath("$.results[0].city") { value("Kapolei") }
            }
        }
    }

    @Test
    fun `get nearby counties`() {
        mockMvc.get("/nearby/counties/{point}", honoluluLatLon) {
        }.andExpect {
            status { isOk() }
            content {
                jsonPath("$.results") { isArray() }
                jsonPath("$.results[0].county") { value("Hawaii") }
                jsonPath("$.results[0].state") { value("Hawaii") }
            }
        }
    }

    @Test
    fun `bad requests`() {
        val singleDimension = "0"
        val threeDimension = "0,0,0"
        val nonNumeric = "a,b"
        mockMvc.get("/nearby/postals/{point}", singleDimension).andExpectBadRequest()
        mockMvc.get("/nearby/counties/{point}", singleDimension).andExpectBadRequest()
        mockMvc.get("/nearby/postals/{point}", threeDimension).andExpectBadRequest()
        mockMvc.get("/nearby/counties/{point}", threeDimension).andExpectBadRequest()
        mockMvc.get("/nearby/postals/{point}", nonNumeric).andExpectBadRequest()
        mockMvc.get("/nearby/counties/{point}", nonNumeric).andExpectBadRequest()
    }
/*
    @Test
    fun `nothing exists near null island`() {
        val nullIsland = "0,0"
        mockMvc.get("/nearby/counties/{point}", nullIsland).andExpectNotFound()
        mockMvc.get("/nearby/postals/{point}", nullIsland).andExpectNotFound()
    }
 */

}

private fun ResultActionsDsl.andExpectBadRequest(): ResultActionsDsl = this.andExpect {
    status { isBadRequest() }
}

private fun ResultActionsDsl.andExpectNotFound(): ResultActionsDsl = this.andExpect {
    status { isNotFound() }
}
