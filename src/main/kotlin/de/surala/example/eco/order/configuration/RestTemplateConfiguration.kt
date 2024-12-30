package de.surala.example.eco.order.configuration

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class RestTemplateConfig(
    private val serviceCatalogProperties: ServiceCatalogProperties
) {
    @Bean
    fun catalogServiceRestTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
            .rootUri(serviceCatalogProperties.url)
            .connectTimeout(Duration.ofSeconds(serviceCatalogProperties.connectTimeout))
            .readTimeout(Duration.ofSeconds(serviceCatalogProperties.readTimeout))
            .build()
    }
}