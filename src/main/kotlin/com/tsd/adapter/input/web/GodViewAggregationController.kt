package com.tsd.adapter.input.web

import com.tsd.core.model.AggregatedPosition
import com.tsd.core.model.GodViewQuery
import com.tsd.core.port.output.AccountBalancePort
import com.tsd.features.portfolio.GodViewAggregationCartridge
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/portfolio")
class GodViewAggregationController(
    private val godViewCartridge: GodViewAggregationCartridge,
    private val accountBalancePort: AccountBalancePort // 🟢 Injected for the isolation test
) {
    @PostMapping("/god-view")
    fun getGodView(@RequestBody query: GodViewQuery): ResponseEntity<List<AggregatedPosition>> {
        val portfolio = godViewCartridge.getAggregatedPortfolio(query)
        return ResponseEntity.ok(portfolio)
    }

    @PostMapping("/isolated-view")
    fun getIsolatedView(
        @RequestBody query: GodViewQuery,
        @RequestHeader(value = "X-Participant-ID", required = false) headerParticipantId: Int?,
        @RequestHeader(value = "X-Registrar-ID", required = false) headerRegistrarId: Int?,
        authentication: Authentication? // 🟢 Spring Security automatically injects the verified token here
    ): ResponseEntity<List<AggregatedPosition>> {

        var resolvedParticipantId = headerParticipantId
        var resolvedRegistrarId = headerRegistrarId

        // 🟢 THE TOGGLE LOGIC:
        // If a verified JWT is present, we completely ignore the fakeable HTTP Headers
        // and extract the undeniable cryptographic truth.
        if (authentication != null && authentication.principal is io.jsonwebtoken.Claims) {
            val claims = authentication.principal as io.jsonwebtoken.Claims
            resolvedParticipantId = claims.get("participant_id", Integer::class.java)?.toInt()
            resolvedRegistrarId = claims.get("registrar_id", Integer::class.java)?.toInt()
        }

        val targetGinId = query.ginId ?: throw IllegalArgumentException("Need GIN_ID for isolated test")

        val portfolio = accountBalancePort.aggregateBalancesWithIsolation(
            targetGinId, resolvedParticipantId, resolvedRegistrarId
        )
        return ResponseEntity.ok(portfolio)
    }
}