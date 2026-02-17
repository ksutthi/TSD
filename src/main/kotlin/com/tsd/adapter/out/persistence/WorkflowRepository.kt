package com.tsd.adapter.out.persistence

/**
 * 📝 The Contract
 * The Engine uses this to save/load state.
 * It has NO dependency on jOOQ or SQL.
 */
interface WorkflowRepository {

    fun save(jobId: String, workflowId: String, currentStep: String, status: String, payloadJson: String)

    fun findStatus(jobId: String): String?

    fun findPayload(jobId: String): String?

    // For recovering crashed jobs on startup
    fun findStuckJobs(): List<String>
}