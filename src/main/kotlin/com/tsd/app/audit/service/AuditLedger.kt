package com.tsd.app.audit.service // 🟢 Note the .service added here!

import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicReference
import org.springframework.stereotype.Component

/**
 * The "General Ledger" Service.
 * Tracks money movement to ensure Zero Data Loss.
 */
@Component
class AuditLedger {
    // Thread-safe accumulators
    private val totalAttempted = AtomicReference(BigDecimal.ZERO)
    private val totalSuccess = AtomicReference(BigDecimal.ZERO)
    private val failureCount = AtomicReference(0)

    fun recordAttempt(amount: BigDecimal) {
        totalAttempted.updateAndGet { it.add(amount) }
    }

    fun recordSuccess(amount: BigDecimal) {
        totalSuccess.updateAndGet { it.add(amount) }
    }

    fun recordFailure() {
        failureCount.updateAndGet { it + 1 }
    }

    fun getReport(): String {
        val attempted = totalAttempted.get()
        val success = totalSuccess.get()

        return """
            ==================================================
            📊  F I N A L   R E C O N C I L I A T I O N
            ==================================================
            💰 Total Value Attempted : $attempted THB
            ✅ Total Value Paid      : $success THB
            ❌ Failed Transactions   : ${failureCount.get()}
            ⚖️  BALANCE CHECK         : ${if (attempted.compareTo(success) == 0) "PERFECT MATCH ✅" else "DISCREPANCY DETECTED 🚨"}
            ==================================================
        """.trimIndent()
    }

    fun reset() {
        totalAttempted.set(BigDecimal.ZERO)
        totalSuccess.set(BigDecimal.ZERO)
        failureCount.set(0)
    }
}