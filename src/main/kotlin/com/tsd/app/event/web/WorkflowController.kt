package com.tsd.app.event.web

import com.tsd.app.authorization.service.OneIdProxy
import com.tsd.platform.spi.WorkflowEngine
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/workflow")
class WorkflowController(
    private val engine: WorkflowEngine,
    private val oneIdProxy: OneIdProxy
) {

    @PostMapping("/execute")
    fun executeWorkflow(@RequestBody payload: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        println("\n📥 API REQUEST RECEIVED: Starting Dynamic Workflow...")

        // --- STEP 1: SECURITY CHECK (The Guard) ---
        val userId = payload["user"]?.toString() ?: "john.doe"
        val isAllowed = oneIdProxy.checkAccess(userId)

        if (!isAllowed) {
            println("⛔ SECURITY BLOCK: Workflow aborted for user $userId")
            return ResponseEntity.status(403).body(mapOf(
                "status" to "Blocked",
                "message" to "Security Policy Denied Access via SET One ID"
            ))
        }

        // --- STEP 2: PREPARE DATA ---
        // Create a mutable map to ensure we can add defaults
        val jobData = payload.toMutableMap()

        // Ensure defaults if missing
        jobData.putIfAbsent("Event_Type", "Cash_Dividend")
        jobData.putIfAbsent("Currency", "THB")

        // Generate a Job ID
        val jobId = "API-${UUID.randomUUID().toString().substring(0, 8)}"

        // --- STEP 3: EXECUTION (The New Way) ---
        // Just pass the ID and the Data! The Engine handles the rest.
        engine.executeJob(jobId, jobData)

        return ResponseEntity.ok(mapOf(
            "status" to "Success",
            "message" to "Workflow executed successfully.",
            "job_id" to jobId,
            "logic_executed" to "Enterprise Matrix"
        ))
    }
}