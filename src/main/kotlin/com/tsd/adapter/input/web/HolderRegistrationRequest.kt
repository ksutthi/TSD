package com.tsd.adapter.input.web

/**
 * The Data Transfer Object (DTO) specifically for the REST API.
 * Keeps the core domain completely ignorant of HTTP and Jackson serialization.
 */
data class HolderRegistrationRequest(
    val queueId: Int,
    val firstName: String,
    val lastName: String,
    val taxId: String?,
    val idType: String,
    val idValue: String,
    val country: String
)