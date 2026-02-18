package com.tsd.adapter.input.web

import com.tsd.adapter.output.identity.OneIdProxy
import com.tsd.adapter.output.persistence.WorkflowRepository
import com.tsd.platform.spi.WorkflowEngine
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/workflows")
class WorkflowController(
    private val engine: WorkflowEngine,
    private val oneIdProxy: OneIdProxy,
    private val repository: WorkflowRepository
) {

    @PostMapping("/execute")
    fun executeWorkflow(@RequestBody request: WorkflowRequest): ResponseEntity<Map<String, Any>> {
        val displayId = request.transactionId ?: "NEW_REQ"
        println("\n📥 API REQUEST RECEIVED: Workflow=${request.workflowId}, Txn=$displayId, Amount=${request.amount} THB")

        // --- STEP 1: SECURITY CHECK ---
        val userId = "admin"
        if (!oneIdProxy.checkAccess(userId)) {
            println("⛔ SECURITY BLOCK: Workflow aborted for user $userId")
            return ResponseEntity.status(403).body(mapOf(
                "status" to "Blocked",
                "message" to "Security Policy Denied Access via SET One ID"
            ))
        }

        // --- STEP 2: IDEMPOTENCY CHECK (Fixed Strategy) ---
        // 1. Determine the Unique Job ID (Transaction ID)
        // If the user sends a transactionId, we use it. If not, we generate a random one.
        val jobId = request.transactionId ?: "API-${UUID.randomUUID().toString().substring(0, 8)}"

        // 2. Check if THIS SPECIFIC TRANSACTION has been processed
        // (We no longer block the whole 'TSD-01' template, only this specific instance)
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
            "Workflow_ID"       to request.workflowId, // This finds the Rules (e.g. "TSD-01")
            "Request_Ref"       to jobId,
            "AMOUNT"            to request.amount,
            "CURRENCY"          to request.currency,
            "INVESTOR_ID"       to request.investorId,
            "WALLET_ID"         to request.walletId,
            "TARGET_BANK"       to request.targetBank,
            "Event_Type"        to "Cash_Dividend",

            // 🟢 Support for Conditional Logic (e.g. "BAHTNET" path)
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
    // 🧪 CHAOS TOOL: Toggle the Identity Service Health
    @PostMapping("/chaos/toggle-identity")
    fun toggleIdentityChaos(@RequestParam down: Boolean): ResponseEntity<String> {
        oneIdProxy.forceSystemDown = down
        return ResponseEntity.ok("💥 Identity Service Down = $down")
    }
}