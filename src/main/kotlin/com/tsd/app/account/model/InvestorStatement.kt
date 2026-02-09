package com.tsd.app.account.model

data class InvestorStatement(
    val gin: String,
    val investorName: String,
    val holdings: List<com.tsd.app.account.model.Holding>,
    val payments: List<com.tsd.app.account.model.PaymentRecord>
)