package com.tsd.app.distribution.model

data class PaymentConfirmRequest(
    val paymentId: Long,
    val referenceNo: String
)