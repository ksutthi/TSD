package com.tsd.adapter.`in`.web

import com.tsd.features.event.EventService
import com.tsd.core.model.EventRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/event")
class EventController(
    private val service: EventService
) {
    @PostMapping("/announce")
    fun announceEvent(@RequestBody request: EventRequest): ResponseEntity<String> {
        service.announceEvent(request)
        return ResponseEntity.ok("✅ Event '${request.securitySymbol}' accepted. Workflows are starting in the background.")
    }
}