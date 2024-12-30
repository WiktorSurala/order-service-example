package de.surala.example.eco.order.configuration

import jakarta.annotation.PostConstruct
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "catalog-service")
data class ServiceCatalogProperties(

    @field:NotBlank(message = "Catalog service URL must not be empty")
    @field:Pattern(
        regexp = "^(http|https)://.*$",
        message = "Catalog service URL must be a valid URL starting with http or https"
    )
    var url: String = "",

    /**
     * Connection timeout in seconds.
     * Default is set to 5 seconds.
     */
    var connectTimeout: Long = 5,

    /**
     * Read timeout in seconds.
     * Default is set to 10 seconds.
     */
    var readTimeout: Long = 5,

    var apiKey: String = ""
) {
    @PostConstruct
    fun init() {
        println("Catalog Service URL: $url")
        println("Connect Timeout: $connectTimeout seconds")
        println("Read Timeout: $readTimeout seconds")
    }
}