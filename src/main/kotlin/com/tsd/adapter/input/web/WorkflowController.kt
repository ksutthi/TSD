package com.tsd.adapter.input.web

import com.tsd.adapter.output.identity.OneIdProxy
import com.tsd.adapter.output.persistence.WorkflowRepository
import com.tsd.platform.spi.WorkflowEngine
import com.tsd.core.service.MakerCheckerWorkflowService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workflows")
class WorkflowController(
    private val engine: WorkflowEngine,
    private val oneIdProxy: OneIdProxy,
    private val repository: WorkflowRepository,
    private val makerCheckerService: MakerCheckerWorkflowService // 🛡️ NEW: Injected your secure service!
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
        // Notice we do NOT ask the UI who the user is!
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
}