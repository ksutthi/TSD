package com.tsd.adapter.input.web

import com.tsd.adapter.output.identity.OneIdProxy
import com.tsd.adapter.output.persistence.WorkflowRepository
import com.tsd.core.service.MakerCheckerWorkflowService
import com.tsd.platform.spi.WorkflowEngine
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workflows")
class WorkflowController(
    private val engine: WorkflowEngine,
    private val oneIdProxy: OneIdProxy,
    private val repository: WorkflowRepository,
    private val makerCheckerService: MakerCheckerWorkflowService,
    @Qualifier("asyncJobLauncher") private val jobLauncher: JobLauncher,
    private val universalEnterpriseBatchJob: Job
) {

    @PostMapping("/execute")
    fun executeWorkflow(@RequestBody request: WorkflowRequest): ResponseEntity<Map<String, Any>> {
        val displayId = request.transactionId ?: "NEW_REQ"
        println("\n📥 API REQUEST RECEIVED: Workflow=${request.workflowId}, Txn=$displayId, Amount=${request.amount} THB")

        // --- STEP 1: SECURITY CHECK (Legacy Placeholder) ---
        val userId = "admin"
        if (!oneIdProxy.checkAccess(userId)) {
            println("⛔ SECURITY BLOCK: Workflow aborted for user $userId")
            return ResponseEntity.status(403).body(mapOf(
                "status" to "Blocked",
                "message" to "Security Policy Denied Access via SET One ID"
            ))
        }

        // --- STEP 2: IDEMPOTENCY CHECK ---
        val jobId = request.transactionId ?: "API-${UUID.randomUUID().toString().substring(0, 8)}"

        if (repository.findStatus(jobId) != null) {
            println("♻️ [Idempotency] Rejecting duplicate Transaction ID: $jobId")
            return ResponseEntity.status(409).body(mapOf(
                "status" to "Conflict",
                "message" to "Duplicate Request: Transaction ID '$jobId' has already been processed.",
                "error_code" to "DUPLICATE_SUBMISSION",
                "job_id" to jobId
            ))
        }

        // --- STEP 3: CONTEXT PREPARATION ---
        val contextData = mapOf(
            "Registrar_Code"    to request.registrar,
            "Workflow_ID"       to request.workflowId,
            "Request_Ref"       to jobId,
            "AMOUNT"            to request.amount,
            "CURRENCY"          to request.currency,
            "INVESTOR_ID"       to request.investorId,
            "WALLET_ID"         to request.walletId,
            "TARGET_BANK"       to request.targetBank,
            "Event_Type"        to "Cash_Dividend",
            "Payment_Mode"      to (request.paymentMode ?: "Standard")
        )

        // 🟢 NEW DEBUG LOG: Check exactly what Jackson parsed before hitting the Engine
        println("   🐞 DEBUG PAYLOAD: Payment Mode is -> ${contextData["Payment_Mode"]}")

        // --- STEP 4: EXECUTION ---
        println("   🚀 Delegating to Engine (Job ID: $jobId)...")
        engine.executeJob(jobId, contextData)

        return ResponseEntity.ok(mapOf(
            "status" to "Success",
            "message" to "Workflow submitted successfully.",
            "job_id" to jobId,
            "link" to "/api/v1/workflows/status/$jobId"
        ))
    }

    @GetMapping("/status/{jobId}")
    fun getJobStatus(@PathVariable jobId: String): ResponseEntity<Map<String, Any>> {
        val status = repository.findStatus(jobId)
        val payload = repository.findPayload(jobId)

        return if (status != null) {
            ResponseEntity.ok(mapOf(
                "job_id" to jobId,
                "status" to status,
                "payload_preview" to (payload?.take(100) ?: "No Data") + "..."
            ))
        } else {
            ResponseEntity.status(404).body(mapOf("error" to "Job Not Found"))
        }
    }

    @PostMapping("/chaos/toggle-identity")
    fun toggleIdentityChaos(@RequestParam down: Boolean): ResponseEntity<String> {
        oneIdProxy.forceSystemDown = down
        return ResponseEntity.ok("💥 Identity Service Down = $down")
    }

    // =========================================================================
    // 🛡️ SECURE MAKER/CHECKER ENDPOINTS
    // =========================================================================

    @PostMapping("/{jobId}/approve")
    fun approveJob(@PathVariable jobId: String): ResponseEntity<Map<String, String>> {

        // This safely checks security, checks the legacy vault, AND wakes the engine!
        makerCheckerService.approveJob(jobId)

        return ResponseEntity.ok(mapOf(
            "status" to "Success",
            "message" to "Job $jobId successfully approved."
        ))
    }

    @PostMapping("/{jobId}/reject")
    fun rejectJob(@PathVariable jobId: String): ResponseEntity<Map<String, String>> {
        makerCheckerService.rejectJob(jobId)
        return ResponseEntity.ok(mapOf(
            "status" to "Success",
            "message" to "Job $jobId successfully rejected."
        ))
    }

    // =========================================================================
    // 📦 HIGH-VOLUME BATCH ENDPOINTS
    // =========================================================================

    @PostMapping("/batch/{registrar}/{workflowId}")
    fun triggerUniversalBatch(
        @PathVariable registrar: String,
        @PathVariable workflowId: String,
        @RequestParam(defaultValue = "100") records: Long
    ): ResponseEntity<Map<String, String>> {

        println("\n🚀 [WorkflowController] Triggering Universal Batch for $registrar / $workflowId ($records records)...")

        // Pass the dynamic routing logic to the Spring Batch Job
        val params = JobParametersBuilder()
            .addString("registrar", registrar)
            .addString("workflowId", workflowId)
            .addLong("record_count", records)
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters()

        val execution = jobLauncher.run(universalEnterpriseBatchJob, params)

        return ResponseEntity.ok(mapOf(
            "status" to "Batch Job Submitted",
            "batch_execution_id" to execution.jobId.toString(),
            "workflow_routed" to workflowId,
            "batch_status" to execution.status.name
        ))
    }
}