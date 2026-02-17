package com.tsd.core.model

data class PaymentConfirmRequest(
    val paymentId: Long,
    val referenceNo: String
)