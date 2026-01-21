package com.csvtojson.models

import kotlinx.serialization.Serializable

@Serializable
data class ConversionMetadata(
    val rows_processed: Int,
    val processing_time_ms: Long,
    val columns: List<String>,
    val timestamp: String,
    val has_header: Boolean
)

@Serializable
data class ConversionResponse(
    val metadata: ConversionMetadata,
    val data: List<Map<String, String>>
)

@Serializable
data class ErrorResponse(
    val error: String,
    val code: String,
    val details: String? = null
)
