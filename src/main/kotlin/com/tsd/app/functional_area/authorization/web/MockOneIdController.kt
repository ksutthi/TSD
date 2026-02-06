package com.tsd.app.functional_area.authorization.web

import com.tsd.app.functional_area.authorization.model.OneIdRequest
import com.tsd.app.functional_area.authorization.model.OneIdResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.core.annotation.Order

@RestController
@RequestMapping("/api/auth")
class MockOneIdController {

    private val logger = LoggerFactory.getLogger(MockOneIdController::class.java)

    @PostMapping("/verify")
    fun verifyIdentity(@RequestBody request: OneIdRequest): ResponseEntity<OneIdResponse> {
        logger.info("🎭 [Mock Server] 📞 Ring Ring! Received verification request for: {}", request.userId)

        val isAllowed = request.userId != "evil_hacker"

        val response = OneIdResponse(
            allowed = isAllowed,
            riskScore = if (isAllowed) 10 else 99,
            reason = if (isAllowed) "Approved by Mock Server" else "Blacklisted User",
            requireMfa = false // 🟢 ADDED THIS FIELD to match your Model
        )

        return ResponseEntity.ok(response)
    }
}