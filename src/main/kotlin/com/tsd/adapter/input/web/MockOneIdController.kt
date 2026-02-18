package com.tsd.adapter.input.web

import com.tsd.adapter.output.identity.OneIdRequest
import com.tsd.adapter.output.identity.OneIdResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class MockOneIdController {

    private val logger = LoggerFactory.getLogger(MockOneIdController::class.java)

    @PostMapping("/verify")
    fun verifyIdentity(@RequestBody request: OneIdRequest): ResponseEntity<OneIdResponse> {
        logger.info("🎭 [Mock Server] 📞 Ring Ring! Received verification request for: {}", request.userId)

        // 🟢 FIX: Block BOTH 'hacker' AND 'evil_hacker' (Case Insensitive)
        val badActors = listOf("hacker", "evil_hacker", "banned_user")
        val isAllowed = !badActors.contains(request.userId.lowercase())

        val response = OneIdResponse(
            allowed = isAllowed,
            riskScore = if (isAllowed) 10 else 99,
            reason = if (isAllowed) "Approved by Mock Server" else "Blacklisted User",
            requireMfa = false
        )

        return ResponseEntity.ok(response)
    }
}