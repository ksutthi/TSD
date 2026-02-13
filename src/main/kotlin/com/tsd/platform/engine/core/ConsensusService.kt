package com.tsd.platform.engine.core

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.persistence.WorkflowRepository
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Service
class ConsensusService(
    private val repository: WorkflowRepository
) {

    // Thread-safe map to hold the "Frozen" futures
    private val pendingAuthorizations = ConcurrentHashMap<String, CompletableFuture<Boolean>>()
    private val voteCounts = ConcurrentHashMap<String, AtomicInteger>()

    // 🟢 RULE: Requires 2 distinct votes to proceed
    private val REQUIRED_VOTES = 2

    fun waitForConsensus(txId: String, amount: String): Boolean {
        println(EngineAnsi.YELLOW + "      ✋ [Consensus] Requesting Approval for Tx: $txId (Amt: $amount)" + EngineAnsi.RESET)

        // 1. Mark as PAUSED in DB
        updateStatus(txId, "PAUSED")

        val future = CompletableFuture<Boolean>()
        pendingAuthorizations[txId] = future
        voteCounts[txId] = AtomicInteger(0)

        try {
            // 2. 🧊 FREEZE THREAD: Wait max 60 seconds for external input
            val result = future.get(60, TimeUnit.SECONDS)

            // 3. If we get here, someone completed the future!
            if (result) updateStatus(txId, "RUNNING")
            return result

        } catch (e: Exception) {
            println(EngineAnsi.RED + "      ⏰ [Consensus] TIMEOUT! No approval received within 60s." + EngineAnsi.RESET)
            updateStatus(txId, "FAILED_TIMEOUT")
            return false
        } finally {
            // Cleanup memory
            pendingAuthorizations.remove(txId)
            voteCounts.remove(txId)
        }
    }

    fun submitVote(txId: String, approver: String): String {
        val future = pendingAuthorizations[txId] ?: return "❌ Transaction $txId is not waiting."

        val currentVotes = voteCounts[txId]?.incrementAndGet() ?: 1
        println(EngineAnsi.CYAN + "      📩 [Consensus] Vote received from $approver. ($currentVotes/$REQUIRED_VOTES)" + EngineAnsi.RESET)

        if (currentVotes >= REQUIRED_VOTES) {
            future.complete(true) // 🔓 UNLOCK THE THREAD
            return "✅ Consensus Reached! Workflow Resumed."
        }

        return "⚠️ Vote Accepted. Waiting for ${REQUIRED_VOTES - currentVotes} more."
    }

    private fun updateStatus(jobId: String, status: String) {
        try {
            // Note: We default to workflow "TSD-01" / "N" here for logging
            val existingPayload = try { repository.findPayload(jobId) } catch (e: Exception) { "{}" }
            repository.save(jobId, "TSD-01", "CONSENSUS_WAIT", status, existingPayload ?: "{}")
        } catch (e: Exception) {
            println(EngineAnsi.RED + "      ⚠️ DB Save Failed: ${e.message}" + EngineAnsi.RESET)
        }
    }
}