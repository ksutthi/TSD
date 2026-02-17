package com.tsd.platform.engine.state

import com.tsd.platform.exception.WorkflowAbortException
import com.tsd.platform.spi.ExecutionContext
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 🧠 KernelContext
 * Thread-safe memory for the workflow execution.
 */
class KernelContext(
    // 1. Core Identity
    val jobId: String = UUID.randomUUID().toString(),
    val tenantId: String = "TSD_DEFAULT",

    // 2. Configuration
    private val systemConfig: Map<String, String> = emptyMap(),

    // 3. SHARED JOB STATE (The "Backpack")
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
            is String -> try {
                BigDecimal(value)
            } catch (e: Exception) { BigDecimal.ZERO }
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
        println("      📝 [$tag] $message")
    }

    override fun getConfig(key: String): String {
        return systemConfig[key] ?: ""
    }

    override fun abort(reason: String) {
        println("      ⛔ [KernelContext] ABORTING JOB: $reason")
        throw WorkflowAbortException(reason)
    }

    // ---------------------------------------------------------
    // 🟢 NEW: Job Accumulator Implementation
    // ---------------------------------------------------------

    override fun getJobState(key: String): Any? {
        return jobState[key]
    }

    override fun setJobState(key: String, value: Any) {
        jobState[key] = value
    }

    // ---------------------------------------------------------
    // ⚠️ Legacy / Specific Helpers
    // These are still here if you need them internally,
    // but Cartridges should prefer using setJobState()
    // ---------------------------------------------------------

    fun addPayoutToJob(accountId: Long, amount: BigDecimal) {
        jobState["PAYOUT_$accountId"] = amount
    }

    fun getAllJobPayouts(): Map<Long, BigDecimal> {
        return jobState.entries
            .filter { it.key.startsWith("PAYOUT_") }
            .associate {
                val id = it.key.removePrefix("PAYOUT_").toLong()
                val amt = it.value as BigDecimal
                id to amt
            }
    }

    fun removePayoutFromJob(accountId: Long) {
        jobState.remove("PAYOUT_$accountId")
    }
}