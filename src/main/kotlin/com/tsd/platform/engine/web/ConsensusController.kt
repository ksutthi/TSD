package com.tsd.platform.engine.web

import com.tsd.platform.engine.core.ConsensusService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/consensus")
class ConsensusController(
    private val consensusService: ConsensusService
) {

    data class VoteRequest(val txId: String, val approver: String)

    @PostMapping("/vote")
    fun castVote(@RequestBody request: VoteRequest): String {
        println("📨 [API] Received Vote from: ${request.approver} for Tx: ${request.txId}")

        // Pass the vote to the Service (which unblocks the thread)
        return consensusService.submitVote(request.txId, request.approver)
    }
}