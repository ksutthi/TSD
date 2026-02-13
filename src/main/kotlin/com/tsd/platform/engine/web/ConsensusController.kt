package com.tsd.platform.engine.web

import com.tsd.platform.engine.core.ConsensusService
import com.tsd.platform.engine.util.EngineAnsi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/consensus")
class ConsensusController(
    private val consensusService: ConsensusService
) {

    // Request DTO
    data class VoteRequest(
        val txId: String,
        val approver: String
    )

    @PostMapping("/vote")
    fun castVote(@RequestBody request: VoteRequest): ResponseEntity<String> {
        println(EngineAnsi.MAGENTA + "📨 [API] Received Vote from: ${request.approver} for Tx: ${request.txId}" + EngineAnsi.RESET)

        try {
            // Pass the vote to the Service (which checks logic and unblocks the thread)
            val result = consensusService.submitVote(request.txId, request.approver)

            return if (result.startsWith("❌")) {
                ResponseEntity.badRequest().body(result)
            } else {
                ResponseEntity.ok(result)
            }
        } catch (e: Exception) {
            return ResponseEntity.internalServerError().body("🔥 Error submitting vote: ${e.message}")
        }
    }
}