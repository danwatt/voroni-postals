package org.danwatt.voronipostals.integration

import org.danwatt.voronipostals.App
import org.junit.runner.RunWith
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.test.context.web.WebAppConfiguration

@RunWith(SpringRunner::class)
@ActiveProfiles("unit")
@ContextConfiguration
@WebAppConfiguration
@TestPropertySource("classpath:application.properties")
abstract class BaseTests {

    @Configuration
    @Profile("unit")
    @Import(App::class)
    class IntegrationTestConfig
}