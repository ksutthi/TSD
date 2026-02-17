package com.tsd.core.model

data class PaymentRecord(
    val isin: String,
    val amount: Double,
    val status: String,
    val date: String
)