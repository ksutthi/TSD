package com.tsd.core.model

data class InvestorStatement(
    val gin: String,
    val investorName: String,
    val holdings: List<Holding>,
    val payments: List<PaymentRecord>
)