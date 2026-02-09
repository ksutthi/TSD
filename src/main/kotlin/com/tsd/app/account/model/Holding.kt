package com.tsd.app.account.model

data class Holding(
    val isin: String,
    val type: String,
    val units: Double
)