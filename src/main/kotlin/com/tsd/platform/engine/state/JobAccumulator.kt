package com.tsd.platform.engine.state

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Component
class JobAccumulator {

    // Partitioned Storage: JobID -> (AccountID -> Amount)
    private val memory = ConcurrentHashMap<String, ConcurrentHashMap<Long, BigDecimal>>()

    // 🟢 SESSION BINDING: Tracks the correct Job ID
    private var activeJobId: String = ""

    // Called by Module J to say "THIS is the real Job ID"
    fun startJobSession(jobId: String) {
        this.activeJobId = jobId
        println("      🧠 [JobAccumulator] Session Bound: $activeJobId")
    }

    fun getActiveJobId(): String {
        return activeJobId
    }

    // --- Standard Methods ---

    fun addPayout(jobId: String, accountId: Long, amount: BigDecimal) {
        // Use incoming ID, or fallback to active ID if incoming is blank
        val idToUse = if (jobId.isNotBlank()) jobId else activeJobId
        val jobData = memory.computeIfAbsent(idToUse) { ConcurrentHashMap() }
        jobData[accountId] = amount
    }

    fun getAllPayouts(jobId: String): Map<Long, BigDecimal> {
        // 🟢 AUTO-FIX: If the incoming ID (e.g. RANDOM-123) has no data, use the Active Session ID
        if (memory.containsKey(jobId)) return memory[jobId]?.toMap() ?: emptyMap()
        return memory[activeJobId]?.toMap() ?: emptyMap()
    }

    fun removePayout(jobId: String, accountId: Long) {
        val targetId = if (memory.containsKey(jobId)) jobId else activeJobId
        memory[targetId]?.remove(accountId)
    }

    // Helper for cleanup (Module Z)
    fun clearJob(jobId: String) {
        memory.remove(jobId)
        if (activeJobId == jobId) activeJobId = ""
    }
}