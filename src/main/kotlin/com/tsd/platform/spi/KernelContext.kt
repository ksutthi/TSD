package com.tsd.platform.spi

import com.tsd.platform.exception.WorkflowAbortException
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 🧠 KernelContext
 * Thread-safe memory for the workflow execution.
 * 🟢 NOW: Implements ExecutionContext to support InstructionCommands.
 */
class KernelContext(
    // 1. Core Identity
    val jobId: String = UUID.randomUUID().toString(),
    val tenantId: String = "TSD_DEFAULT",

    // 2. Configuration (Optional, for getConfig)
    private val systemConfig: Map<String, String> = emptyMap()
) : ExecutionContext {

    // 3. Thread-Safe Storage
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
}