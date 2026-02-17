package com.tsd.features.event

import com.tsd.adapter.out.persistence.AccountBalanceRepository
import com.tsd.core.model.EventRequest
import com.tsd.platform.spi.WorkflowEngine
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID
import java.math.BigDecimal

@Service
class EventService(
    private val accountRepo: AccountBalanceRepository,
    private val engine: WorkflowEngine
) {

    @Async
    fun announceEvent(req: EventRequest) {
        println("\n📢 [SERVICE] Received Request: ${req.eventType}")

        // 1. 🧪 TEST MODE (Chaos Monkey)
        // Only runs if the user specifically requested Workflow "T"
        if (req.workflowId == "T") {
            println("   🧪 [Test Mode] Executing Single Job for Chaos Monkey...")

            val jobId = req.jobId ?: "TEST-${UUID.randomUUID()}"

            val payload = mutableMapOf<String, Any>(
                "Workflow_ID" to "T",
                "Registrar_Code" to "TEST", // 🟢 Must match CSV "TEST"
                "Event_Type" to req.eventType,
                "Security_Symbol" to req.securitySymbol,
                "CHAOS_MODE" to "ALL"
            )

            engine.executeJob(jobId, payload)
            return
        }

        // 2. 🏢 MASS DISTRIBUTION MODE (Real Business Logic)
        println("   🏢 [Mass Mode] Starting Dividend Distribution Loop...")

        val accounts = accountRepo.findAll()
        var count = 0

        accounts.forEach { account ->
            try {
                val txnId = "TXN-${account.accountId}-" + UUID.randomUUID().toString().substring(0, 8)
                val payload = mutableMapOf<String, Any>()

                // 🟢 CRITICAL FIX: Match the CSV Keys exactly!
                payload["Workflow_ID"] = "TSD-01"   // Was "N"
                payload["Registrar_Code"] = "TSD"   // Required for lookup

                // 🟢 LOGIC: Different payment paths
                // This simulates the Decision Logic in the CSV (SWIFT vs BAHTNET)
                if (account.accountId == 5L) {
                    // 💰 Billionaire: 25 Million THB -> SWIFT (High Value)
                    payload["Net_Amount"] = BigDecimal("25000000.00")
                    payload["Payment_Mode"] = "SWIFT"
                } else {
                    // 👤 Regular Joe: 500 THB -> BAHTNET/PromptPay
                    payload["Net_Amount"] = BigDecimal("500.00")
                    payload["Payment_Mode"] = "BAHTNET"
                }

                engine.executeJob(txnId, payload)
                count++

            } catch (e: Exception) {
                println("   🔥 Error processing account ${account.accountId}: ${e.message}")
            }
        }
        println("✅ [SERVICE] Sent $count transactions to Settlement Engine.")
    }
}