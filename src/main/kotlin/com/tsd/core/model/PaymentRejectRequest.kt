package com.tsd.core.model

data class PaymentRejectRequest(
    val paymentId: Long,
    val reason: String
)