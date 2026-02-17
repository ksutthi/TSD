package com.tsd.adapter.out.identity

data class OneIdResponse(
    val allowed: Boolean,
    val riskScore: Int,
    val reason: String,
    val requireMfa: Boolean
)