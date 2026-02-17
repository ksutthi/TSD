package com.tsd.core.model

data class EventRequest(
    // 🟢 EXISTING BUSINESS FIELDS (Required)
    val eventType: String,
    val securitySymbol: String,
    val bookClosingDate: String,
    val rate: Double,
    val currency: String,

    // 🟢 NEW CONTROL FIELDS (Optional / Nullable)
    // We add these so we can inject "Job_ID" and "Workflow_ID" from Postman
    val workflowId: String? = null,
    val jobId: String? = null,
    val registrarCode: String? = null
)