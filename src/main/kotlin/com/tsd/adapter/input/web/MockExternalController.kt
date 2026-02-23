package com.tsd.adapter.input.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/mock-external")
class MockExternalController {

    var forceOutage = false // 😈 The Global Chaos Switch

    // 🟢 Generalized Endpoint: Accepts ANY method and ANY payload
    @RequestMapping(
        value = ["/endpoint"],
        method = [RequestMethod.POST, RequestMethod.GET, RequestMethod.PUT]
    )
    fun receiveCall(@RequestBody(required = false) payload: Map<String, Any>?): ResponseEntity<String> {

        // 1. Simulate standard network latency (100ms)
        Thread.sleep(100)

        // 2. Simulate a catastrophic external API failure
        if (forceOutage) {
            println("      🌩️ [MockExternal] Simulating 500 Internal Server Error!")
            return ResponseEntity.status(500).body("Internal API Error")
        }

        // 3. Happy Path
        return ResponseEntity.ok("Received Generic Payload: ${payload?.keys ?: "No Body"}")
    }

    // 🟢 The Chaos Toggle (Use this in Postman to break the network)
    @PostMapping("/chaos")
    fun toggleChaos(@RequestParam down: Boolean): ResponseEntity<String> {
        forceOutage = down
        val status = if (down) "DOWN (Returning 500s)" else "UP (Returning 200s)"
        println("\n😈 [Chaos Monkey] Mock External API is now $status\n")
        return ResponseEntity.ok("External Mock is now $status")
    }
}