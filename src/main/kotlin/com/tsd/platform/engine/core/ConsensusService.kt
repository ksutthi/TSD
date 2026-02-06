package com.tsd.platform.engine.core // 🟢 Same package as the Engine

import com.tsd.platform.engine.util.EngineAnsi
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Service
class ConsensusService {

    // 🧠 Memory: Stores the "Listening Threads" (Futures)
    private val pendingAuthorizations = ConcurrentHashMap<String, CompletableFuture<Boolean>>()

    // 🗳️ Ballot Box: Counts votes
    private val voteCounts = ConcurrentHashMap<String, AtomicInteger>()

    private val REQUIRED_VOTES = 2

    /**
     * 🛑 BLOCKING METHOD
     */
    fun waitForConsensus(txId: String, amount: String): Boolean {
        println(EngineAnsi.YELLOW + "      ✋ [Consensus] Requesting Approval for Tx: $txId (Amt: $amount)" + EngineAnsi.RESET)
        println(EngineAnsi.YELLOW + "      ⏳ [Consensus] Thread PAUSED... Waiting for $REQUIRED_VOTES external votes..." + EngineAnsi.RESET)

        val future = CompletableFuture<Boolean>()
        pendingAuthorizations[txId] = future
        voteCounts[txId] = AtomicInteger(0)

        try {
            // 🕒 Wait max 60 seconds
            return future.get(60, TimeUnit.SECONDS)
        } catch (e: Exception) {
            println(EngineAnsi.RED + "      ⏰ [Consensus] TIMEOUT! No approval received." + EngineAnsi.RESET)
            return false
        } finally {
            pendingAuthorizations.remove(txId)
            voteCounts.remove(txId)
        }
    }

    /**
     * ✅ UNBLOCKING METHOD
     */
    fun submitVote(txId: String, approver: String): String {
        val future = pendingAuthorizations[txId] ?: return "❌ Transaction $txId is not waiting."

        val currentVotes = voteCounts[txId]?.incrementAndGet() ?: 1

        println(EngineAnsi.CYAN + "      📩 [Consensus] Vote received from $approver. ($currentVotes/$REQUIRED_VOTES)" + EngineAnsi.RESET)

        if (currentVotes >= REQUIRED_VOTES) {
            future.complete(true)
            return "✅ Consensus Reached! Workflow Resumed."
        }

        return "⚠️ Vote Accepted. Waiting for ${REQUIRED_VOTES - currentVotes} more."
    }
}