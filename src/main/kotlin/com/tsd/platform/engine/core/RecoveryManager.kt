package com.tsd.platform.engine.core

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class RecoveryManager(
    private val jdbcTemplate: JdbcTemplate
) {

    @EventListener(ApplicationReadyEvent::class)
    fun scanForCrashedJobs() {
        println("\n" + "=".repeat(50))
        println("🚑 [Lazarus] System Startup Recovery Scan...")

        // 🟢 QUERY THE JOURNAL (Not just the Audit Log)
        // We look for jobs in 'PENDING' status in the Journal.
        // This implies the engine crashed before marking them SETTLED or FAILED.
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
                    val payload = row["Payload"] as String

                    println("      ⚠️ Job: $jobId")
                    println("      💾 SAVED DATA: $payload")
                    println("      🔄 ACTION: Auto-Requeueing... (Simulated)")

                    // 🟢 SIMULATE RECOVERY
                    // In a real scenario, you would call: engine.executeJob(jobId, parse(payload))
                    // For now, we update the status so we don't loop forever.

                    jdbcTemplate.update("UPDATE Workflow_Journal SET Status = 'RECOVERED' WHERE Job_ID = ?", jobId)

                    println("      ✨ Job marked as RECOVERED.")
                }
            }

        } catch (e: Exception) {
            println("   ⚠️ [Lazarus] DB error or Journal table missing: ${e.message}")
        }
        println("=".repeat(50) + "\n")
    }
}