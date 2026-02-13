package com.tsd.app.event.service

import com.tsd.app.account.repo.AccountBalanceRepository
import com.tsd.app.event.web.EventRequest
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
    fun announceEvent(request: EventRequest) {
        println("\n📢 [SERVICE] Processing Event (Target: Distribution Module N)")

        val accounts = accountRepo.findAll()
        var count = 0

        accounts.forEach { account ->
            try {
                val jobId = "TXN-${account.accountId}-" + UUID.randomUUID().toString().substring(0, 8)

                val payload = mutableMapOf<String, Any>()

                // 🟢 TARGET WORKFLOW "N" (Distribution) - Where the Money/Pause is!
                payload["Workflow_ID"] = "N"
                payload["Tenant_ID"] = "TSD-01"

                // 🟢 ROUTING KEYS
                payload["Payment_Mode"] = "eDividend"

                // 🟢 INJECT THE CASH (Simulate Calculation Result)
                if (account.accountId == 5L) {
                    // Billionaire: 25 Million THB
                    payload["Net_Amount"] = BigDecimal("25000000.00")
                } else {
                    // Regular Joe: 500 THB
                    payload["Net_Amount"] = BigDecimal("500.00")
                }

                // Fire!
                engine.executeJob(jobId, payload)
                count++

            } catch (e: Exception) {
                println("   🔥 Error: ${e.message}")
            }
        }
        println("✅ [SERVICE] Sent $count transactions to Settlement Engine.")
    }
}