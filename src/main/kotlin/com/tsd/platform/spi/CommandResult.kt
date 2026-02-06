package com.tsd.platform.spi

/**
 * Standardized feedback for the multi-tenant engine.
 */
data class CommandResult(
    val success: Boolean,
    val message: String
)