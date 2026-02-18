package com.tsd.adapter.input.web // 🟢 FIX: Backticks around `in`
import com.fasterxml.jackson.annotation.JsonProperty // 🟢 THIS IMPORT WAS MISSING

data class WorkflowRequest(
    val registrar: String,
    val workflowId: String, // The Template (e.g. "TSD-01")

    // 🟢 NEW: The Unique Business Key (e.g. "TXN-1002")
    // If null, we generate one, but then Idempotency is weaker.
    val transactionId: String? = null,

    val amount: Double,
    val currency: String,
    val investorId: String,
    val walletId: String,
    val targetBank: String,

    // Optional extra fields
    @JsonProperty("Payment_Mode")
    val paymentMode: String? = null
)
