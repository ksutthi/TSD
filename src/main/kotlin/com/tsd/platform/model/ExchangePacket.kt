package com.tsd.platform.model

import java.util.UUID

data class ExchangePacket(
    // 🟢 NEW: The specific Item ID (e.g., "Account_123")
    // Default to a random UUID if not provided (e.g. for Job-level packets)
    val id: String = UUID.randomUUID().toString(),

    // 🟢 RETAINED: The Global Trace ID for the whole workflow
    val traceId: String = UUID.randomUUID().toString(),

    // Data Payload
    val data: MutableMap<String, Any> = mutableMapOf(),

    // Metadata (Status flags, etc.)
    val metadata: MutableMap<String, String> = mutableMapOf()
)