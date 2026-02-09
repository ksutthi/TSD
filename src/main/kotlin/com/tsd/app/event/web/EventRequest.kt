package com.tsd.app.event.web

data class EventRequest(
    val eventType: String,
    val securitySymbol: String,
    val bookClosingDate: String,
    val rate: Double,
    val currency: String
)