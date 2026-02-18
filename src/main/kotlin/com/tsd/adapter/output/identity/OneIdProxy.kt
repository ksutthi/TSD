package com.tsd.adapter.output.identity

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker // 🟢 NEW IMPORT
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class OneIdProxy(
    private val restTemplate: RestTemplate,
    @Value("\${integration.one-id.url:http://localhost:8080/api/auth/verify}")
    private val oneIdUrl: String
) {

    // 🟢 1. CHAOS SWITCH (Required for Controller)
    var forceSystemDown: Boolean = false

    companion object {
        private val logger = LoggerFactory.getLogger(OneIdProxy::class.java)
    }

    // 🟢 2. ANNOTATION: Connects this method to Resilience4j
    @CircuitBreaker(name = "identity-service", fallbackMethod = "fallbackAccess")
    fun checkAccess(user: String): Boolean {
        logger.info("📞 [TSD Registry] Calling SET One ID for user: {}...", user)

        // 🟢 3. CHAOS CHECK: Simulate failure if switch is ON
        if (forceSystemDown) {
            logger.error("💥 [OneID] SIMULATED FAILURE: Connection Timeout!")
            throw RuntimeException("Simulated Identity Service Failure")
        }

        // --- 🟢 TESTING BACKDOOR ---
        if (user.equals("admin", ignoreCase = true) || user.equals("system", ignoreCase = true)) {
            logger.warn("✅ [TSD Registry] TEST MODE: Granting automatic access to '{}'", user)
            return true
        }

        // 🟢 4. REMOVED TRY-CATCH
        // We MUST let the exception happen so the Circuit Breaker detects it.
        // If we catch it here, the Circuit Breaker thinks everything is fine.

        val request = OneIdRequest(userId = user)

        val response = restTemplate.postForObject(
            oneIdUrl,
            request,
            OneIdResponse::class.java
        )

        if (response != null && response.allowed) {
            logger.info("✅ [TSD Registry] One ID Approved! (Risk: {})", response.riskScore)
            return true
        } else {
            logger.warn("⛔ [TSD Registry] One ID Denied: {}", response?.reason)
            return false
        }
    }

    // 🟢 5. FALLBACK METHOD
    // This runs when the Circuit Breaker is OPEN or the network fails
    fun fallbackAccess(user: String, t: Throwable): Boolean {
        logger.error("🛡️ [Resilience] CIRCUIT OPEN! Reason: {}", t.message)

        // FAIL SAFE: Deny access if ID system is down
        return false
    }
}