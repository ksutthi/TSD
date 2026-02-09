package com.tsd.app.authorization.model

data class OneIdResponse(
    val allowed: Boolean,
    val riskScore: Int,
    val reason: String,
    val requireMfa: Boolean
)