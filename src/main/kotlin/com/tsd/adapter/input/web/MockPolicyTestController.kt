package com.tsd.adapter.input.web

import com.tsd.core.model.PlatformAction
import com.tsd.core.port.output.PolicyEnginePort
import com.tsd.core.port.output.SecurityContextPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/test/policy")
@Profile("local", "test")
class MockPolicyTestController(
    private val securityPort: SecurityContextPort,
    private val policyPort: PolicyEnginePort
) {
    private val log = LoggerFactory.getLogger(MockPolicyTestController::class.java)

    @GetMapping("/transfer")
    fun attemptTransfer(@RequestParam amount: String): ResponseEntity<String> {
        try {
            val transferAmount = BigDecimal(amount)

            // 1. Get the "Who"
            val currentUser = securityPort.getCurrentUser()
            log.info("👤 [TEST ENDPOINT] Current User: ${currentUser.fullName} (${currentUser.role})")

            // 2. Ask "Can They?"
            val isAllowed = policyPort.isAuthorized(currentUser, PlatformAction.INITIATE_TRANSFER, transferAmount)

            return if (isAllowed) {
                ResponseEntity.ok("✅ Transfer Initiated Successfully for $transferAmount THB")
            } else {
                ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Access Denied: Insufficient authority to initiate $transferAmount THB")
            }
        } catch (e: Exception) {
            // 🚨 FORCE THE ERROR INTO THE CONSOLE BEFORE THE GLOBAL HANDLER HIDES IT!
            log.error("💥 CRASH IN POLICY TEST CONTROLLER!", e)
            throw e
        }
    }
}