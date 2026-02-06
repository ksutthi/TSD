package com.tsd.app.functional_area.event.service

import com.tsd.app.functional_area.account.repo.AccountBalanceRepository
import com.tsd.app.functional_area.event.web.EventRequest
import com.tsd.platform.spi.WorkflowEngine
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EventService(
    private val accountRepo: AccountBalanceRepository,
    private val engine: WorkflowEngine
) {

    // 🟢 @Async means the API returns immediately, while this runs in background
    @Async
    fun announceEvent(request: EventRequest) {
        println("\n📢 [SERVICE] Processing Event Announcement: ${request.eventType}")

        val accounts = accountRepo.findAll()
        var count = 0

        accounts.forEach { account ->
            try {
                val jobId = "EVT-${account.accountId}-" + UUID.randomUUID().toString().substring(0, 8)

                val payload = mutableMapOf<String, Any>()
                payload["Event_Type"] = request.eventType
                payload["Security_Symbol"] = request.securitySymbol
                payload["Dividend_Rate"] = request.rate
                payload["Account_ID"] = account.accountId

                // 🧪 HACK: Billionaire Test
                if (account.accountId == 5L) {
                    payload["Share_Balance"] = 5_000_000.0
                } else {
                    payload["Share_Balance"] = account.shareBalance.toDouble()
                }

                engine.executeJob(jobId, payload)
                count++

            } catch (e: Exception) {
                println("   🔥 Failed to process Account ${account.accountId}: ${e.message}")
            }
        }
        println("✅ [SERVICE] Finished processing $count accounts.")
    }
}