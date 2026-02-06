package com.tsd.app.functional_area.account.model

data class PaymentRecord(
    val isin: String,
    val amount: Double,
    val status: String,
    val date: String
)