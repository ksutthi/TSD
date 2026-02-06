package com.tsd.app.functional_area.account.model

data class BatchAccountResult(
    val accountId: String,
    val globalEntityId: String,
    val status: String,
    val riskScore: Int
)