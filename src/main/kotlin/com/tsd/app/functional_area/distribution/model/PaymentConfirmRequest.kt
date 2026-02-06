package com.tsd.app.functional_area.distribution.model

data class PaymentConfirmRequest(
    val paymentId: Long,
    val referenceNo: String
)