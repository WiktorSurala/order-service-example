package de.surala.example.eco.order.configuration

import jakarta.annotation.PostConstruct
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @PostConstruct
    fun init() {
        logger.debug("Catalog Service URL: $url")
        logger.debug("Connect Timeout: $connectTimeout seconds")
        logger.debug("Read Timeout: $readTimeout seconds")
    }
}