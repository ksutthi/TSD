package com.tsd.core.model

data class BatchAccountResult(
    val accountId: String,
    val globalEntityId: String,
    val status: String,
    val riskScore: Int
)