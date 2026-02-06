package com.tsd.app.functional_area.authorization.model

data class OneIdResponse(
    val allowed: Boolean,
    val riskScore: Int,
    val reason: String,
    val requireMfa: Boolean
)