package com.tsd.adapter.input.web

import com.tsd.features.identity.IdentityResolutionCartridge
import com.tsd.features.identity.IncomingHolderRecord
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/identity")
class IdentityResolutionController(
    // 🟢 Injecting the Core Brain
    private val identityResolutionCartridge: IdentityResolutionCartridge
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/resolve")
    fun resolveIdentity(@RequestBody request: HolderRegistrationRequest): ResponseEntity<Map<String, Any>> {
        log.info("Received HTTP request to resolve identity for Queue ID: ${request.queueId}")

        // 1. Translate the HTTP Payload into the pure Domain Object
        val record = IncomingHolderRecord(
            queueId = request.queueId,
            firstName = request.firstName,
            lastName = request.lastName,
            taxId = request.taxId,
            idType = request.idType,
            idValue = request.idValue,
            country = request.country
        )

        // 2. Trigger the Core Engine
        val ginId = identityResolutionCartridge.resolveIdentity(record)

        // 3. Return the exact Tier 1 Anchor ID to the caller
        return ResponseEntity.ok(
            mapOf(
                "status" to "SUCCESS",
                "message" to "Identity resolved and linked to Golden Record.",
                "globalInvestorId" to ginId
            )
        )
    }
}