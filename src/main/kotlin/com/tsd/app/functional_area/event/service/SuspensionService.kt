package com.tsd.app.functional_area.event.service

import com.tsd.app.functional_area.event.web.ResolutionRequest
import com.tsd.platform.persistence.SuspendedActionRepository
import com.tsd.platform.spi.WorkflowEngine
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class SuspensionService(
    private val repo: SuspendedActionRepository,
    private val engine: WorkflowEngine
) {

    @Transactional
    fun resolveSuspension(request: ResolutionRequest): String {
        // 1. Logic: Find and Validate
        val suspendedItem = repo.findByAccountIdAndCartridgeName(request.accountId, "Call_Identity_Mgmt")
            ?: return "❌ No suspension found for Account ${request.accountId}"

        // 2. Logic: Update Status
        suspendedItem.status = if (request.action == "APPROVE") "APPROVED" else "REJECTED"
        suspendedItem.updatedAt = LocalDateTime.now()
        repo.save(suspendedItem)

        // 3. Logic: Decide next step
        if (request.action == "REJECT") {
            return "⛔ Transaction Rejected. Workflow Terminated."
        }

        println("\n🔄 [SERVICE] Resuming Workflow for Account ${request.accountId}...")

        // 4. Logic: Call Engine
        val jobId = "RESUME-" + UUID.randomUUID().toString().substring(0, 8)
        engine.executeJob(jobId, suspendedItem.contextData.toMutableMap())

        return "✅ Account ${request.accountId} Resumed Successfully!"
    }
}