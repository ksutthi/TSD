package com.tsd.app.functional_area.event.web

data class EventRequest(
    val eventType: String,
    val securitySymbol: String,
    val bookClosingDate: String,
    val rate: Double,
    val currency: String
)