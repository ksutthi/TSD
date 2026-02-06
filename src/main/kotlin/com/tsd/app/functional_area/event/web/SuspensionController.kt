package com.tsd.app.functional_area.event.web

import com.tsd.app.functional_area.event.service.SuspensionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/suspension")
class SuspensionController(
    private val service: SuspensionService
) {
    @PostMapping("/resolve")
    fun resolveSuspension(@RequestBody request: ResolutionRequest): ResponseEntity<String> {
        val result = service.resolveSuspension(request)
        return ResponseEntity.ok(result)
    }
}