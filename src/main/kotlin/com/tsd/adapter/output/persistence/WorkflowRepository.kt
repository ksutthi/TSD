package com.tsd.adapter.output.persistence

import com.tsd.core.model.AuditLog

interface WorkflowRepository {
    // Basic CRUD
    fun save(jobId: String, workflowId: String, currentStep: String, status: String, payloadJson: String)
    fun findStatus(jobId: String): String?
    fun findPayload(jobId: String): String?
    fun findStuckJobs(): List<String>

    // Idempotency Check (Business)
    fun existsByWorkflowId(workflowId: String): Boolean

    // 🟢 NEW: Idempotency Check (Step Level) - Refactored from Engine
    fun isStepAlreadyDone(jobId: String, stepCode: String): Boolean
}