package com.tsd.app.functional_area.distribution.model

data class PaymentRejectRequest(
    val paymentId: Long,
    val reason: String
)