package com.tsd.app.authorization.model

data class OneIdRequest(
    val userId: String,
    val service: String = "REGISTRY",
    val function: String = "APPROVE_PAYMENT",
    val role: String = "MAKER",
    val amount: Double = 0.0,
    val channelId: String = "WEB_INTERNAL",
    val deviceTrust: Int = 100,
    val ipAddress: String = "127.0.0.1"
)