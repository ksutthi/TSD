package com.tsd.platform.spi

import com.tsd.platform.exception.WorkflowAbortException
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 🧠 KernelContext
 * Thread-safe memory for the workflow execution.
 * 🟢 NOW: Supports 'Job Accumulator' pattern for in-memory state passing.
 */
class KernelContext(
    // 1. Core Identity
    val jobId: String = UUID.randomUUID().toString(),
    val tenantId: String = "TSD_DEFAULT",

    // 2. Configuration (Optional, for getConfig)
    private val systemConfig: Map<String, String> = emptyMap(),

    // 3. 🟢 SHARED JOB STATE (The "Backpack")
    // This map is shared across the entire Job. It survives when the Item loop finishes.
    val jobState: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
) : ExecutionContext {

    // 4. Thread-Safe Storage (Local Item Scope)
    private val memory: MutableMap<String, Any> = ConcurrentHashMap()

    // ---------------------------------------------------------
    // 🔌 ExecutionContext Implementation
    // ---------------------------------------------------------

    override fun getTenantID(): String = tenantId
    override fun getEventID(): String = jobId

    // --- Data Accessors ---

    override fun getString(key: String): String {
        // Contract says: Must return String (not null). Default to empty if missing.
        return memory[key]?.toString() ?: ""
    }

    override fun getAmount(key: String): BigDecimal {
        val value = memory[key]
        return when (value) {
            is BigDecimal -> value
            is Double -> BigDecimal.valueOf(value)
            is Float -> BigDecimal.valueOf(value.toDouble())
            is Int -> BigDecimal.valueOf(value.toLong())
            is Long -> BigDecimal.valueOf(value)
            is String -> try { BigDecimal(value) } catch (e: Exception) { BigDecimal.ZERO }
            else -> BigDecimal.ZERO
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getObject(key: String): T? {
        return memory[key] as? T
    }

    // --- Data Mutators ---

    override fun set(key: String, value: Any) {
        memory[key] = value
    }

    // --- Operations ---

    override fun log(tag: String, message: String) {
        // Simple console logging for now
        println("      📝 [$tag] $message")
    }

    override fun getConfig(key: String): String {
        return systemConfig[key] ?: ""
    }

    override fun abort(reason: String) {
        println("      ⛔ [KernelContext] ABORTING JOB: $reason")
        // We throw a specific exception that the engine can catch to stop the flow
        throw WorkflowAbortException(reason)
    }

    // ---------------------------------------------------------
    // 🟢 NEW: Job Accumulator Helpers (The "Proper" Fix)
    // ---------------------------------------------------------

    /**
     * Save a calculated payout to the Job-Level memory.
     * This survives the Item Scope (doesn't get deleted after the loop).
     */
    fun addPayoutToJob(accountId: Long, amount: BigDecimal) {
        // We prefix keys to avoid collisions with other job data
        jobState["PAYOUT_$accountId"] = amount
    }

    /**
     * Retrieve ALL payouts stored in the Job-Level memory.
     * Used by Bulk/Batch Cartridges (e.g. PDF Gen, Bank File).
     */
    fun getAllJobPayouts(): Map<Long, BigDecimal> {
        return jobState.entries
            .filter { it.key.startsWith("PAYOUT_") }
            .associate {
                val id = it.key.removePrefix("PAYOUT_").toLong()
                val amt = it.value as BigDecimal
                id to amt
            }
    }

    /**
     * Mark a payout as processed (remove from memory).
     */
    fun removePayoutFromJob(accountId: Long) {
        jobState.remove("PAYOUT_$accountId")
    }
}