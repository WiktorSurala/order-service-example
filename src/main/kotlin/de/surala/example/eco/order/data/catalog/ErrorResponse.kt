package de.surala.example.eco.order.data.catalog

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class ErrorResponse @JsonCreator constructor(
    @JsonProperty("error") val error: String?,
    @JsonProperty("message") val message: String?
)