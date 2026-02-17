package com.tsd.core.model

data class ResolutionRequest(
    val accountId: Long,
    val action: String // "APPROVE" or "REJECT"
)