package com.tsd.platform.engine.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.tsd.platform.spi.ExchangePacket // ✅ Fixed Import
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class RecoveryManager(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val engine: EnterpriseWorkflowEngine // ✅ Correct (Same Package, no import needed)
) {

    @EventListener(ApplicationReadyEvent::class)
    fun scanForCrashedJobs() {
        println("\n" + "=".repeat(50))
        println("🚑 [Lazarus] System Startup Recovery Scan...")

        // 🟢 QUERY THE JOURNAL
        // We look for jobs in 'PENDING' status.
        val sql = """
            SELECT Job_ID, Payload 
            FROM Workflow_Journal 
            WHERE Status = 'PENDING' 
            AND Created_At > DATEADD(day, -1, GETDATE())
        """

        try {
            val stuckJobs = jdbcTemplate.queryForList(sql)

            if (stuckJobs.isEmpty()) {
                println("   ✅ [Lazarus] No crashed jobs found in Journal. System is clean.")
            } else {
                println("   🚨 [Lazarus] FOUND ${stuckJobs.size} CRASHED JOBS WITH PAYLOAD!")

                stuckJobs.forEach { row ->
                    val jobId = row["Job_ID"] as String
                    val payloadJson = row["Payload"] as String

                    println("      ⚠️ Recovering Job: $jobId")

                    try {
                        // 1. Deserialize the JSON back to a Map
                        @Suppress("UNCHECKED_CAST")
                        val payloadMap = objectMapper.readValue(payloadJson, Map::class.java) as MutableMap<String, Any>

                        // 2. Reconstruct the Packet
                        // We reuse the ORIGINAL Job ID so the Idempotency Check works!
                        val packet = ExchangePacket(
                            id = jobId,       // Restores the original Job ID
                            traceId = jobId,  // Restores the original Trace ID
                            data = payloadMap
                        )

                        // 3. RESURRECT: Send it back to the Engine
                        println("      🔄 ACTION: Re-injecting into Engine...")

                        // The Engine will now run. It will:
                        // - Check 'isStepAlreadyDone' -> Skip finished steps
                        // - Run remaining steps
                        // - Update Status to 'SETTLED' when done
                        engine.executeJob(jobId, payloadMap)

                        println("      ✨ Job $jobId recovered and processing complete.")

                    } catch (e: Exception) {
                        println("      ❌ [Lazarus] Failed to recover $jobId: ${e.message}")
                        // Optional: Mark as FAILED so we don't retry forever
                        // jdbcTemplate.update("UPDATE Workflow_Journal SET Status = 'FAILED' WHERE Job_ID = ?", jobId)
                    }
                }
            }

        } catch (e: Exception) {
            println("   ⚠️ [Lazarus] DB error or Journal table missing: ${e.message}")
        }
        println("=".repeat(50) + "\n")
    }
}