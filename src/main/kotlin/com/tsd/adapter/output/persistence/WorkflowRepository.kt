package com.tsd.adapter.output.persistence

// 🟢 Import your new Port here
import com.tsd.core.port.output.WorkflowStatePort

interface WorkflowRepository : WorkflowStatePort {

    // 🟢 We add 'override' here to prove this adapter fulfills the Core Port's contract
    override fun save(jobId: String, workflowId: String, currentStep: String, status: String, payloadJson: String)
    override fun isStepAlreadyDone(jobId: String, stepCode: String): Boolean

    // Basic CRUD (Specific to this Adapter)
    fun findStatus(jobId: String): String?
    fun findPayload(jobId: String): String?
    fun findStuckJobs(): List<String>

    // Idempotency Check (Business)
    fun existsByWorkflowId(workflowId: String): Boolean
}