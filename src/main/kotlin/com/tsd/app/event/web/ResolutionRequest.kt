package com.tsd.app.event.web

data class ResolutionRequest(
    val accountId: Long,
    val action: String // "APPROVE" or "REJECT"
)