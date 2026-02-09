package com.tsd.app.distribution.model

data class PaymentRejectRequest(
    val paymentId: Long,
    val reason: String
)