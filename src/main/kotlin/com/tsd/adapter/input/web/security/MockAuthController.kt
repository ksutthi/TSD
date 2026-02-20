package com.tsd.adapter.input.web.security

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class MockAuthController(private val jwtService: JwtService) {

    @PostMapping("/login/kgi")
    fun loginKgi(): Map<String, String> {
        // Generates a token specifically locked to Participant 13 (KGI)
        val token = jwtService.generateToken("kgi-system", "ROLE_BROKER", 13, null)
        return mapOf("token" to token)
    }

    @PostMapping("/login/scb")
    fun loginScb(): Map<String, String> {
        // Generates a token specifically locked to Participant 45 (SCB)
        val token = jwtService.generateToken("scb-system", "ROLE_BROKER", 45, null)
        return mapOf("token" to token)
    }

    @PostMapping("/login/admin")
    fun loginAdmin(): Map<String, String> {
        // Generates a super-admin token with no participant limits
        val token = jwtService.generateToken("tsd-admin", "ROLE_TSD_ADMIN", null, null)
        return mapOf("token" to token)
    }
}