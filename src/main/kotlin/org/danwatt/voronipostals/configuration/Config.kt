package org.danwatt.voronipostals.configuration

import com.vividsolutions.jts.geom.GeometryFactory
import org.danwatt.voronipostals.repository.PostalSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Config {
    @Bean
    fun postalSource(@Value("\${voroni.postal.file}") postalFile: String) = PostalSource.load(postalFile)

    @Bean
    fun geometryFactory() = GeometryFactory()
}