package com.tsd.app.functional_area.event.web

data class ResolutionRequest(
    val accountId: Long,
    val action: String // "APPROVE" or "REJECT"
)