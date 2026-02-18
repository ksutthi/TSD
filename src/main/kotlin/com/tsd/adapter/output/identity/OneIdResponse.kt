package com.tsd.adapter.output.identity

data class OneIdResponse(
    val allowed: Boolean,
    val riskScore: Int,
    val reason: String,
    val requireMfa: Boolean
)