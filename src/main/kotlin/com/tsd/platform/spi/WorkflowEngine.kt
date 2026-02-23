package com.tsd.platform.spi

interface WorkflowEngine {
    // 🟢 The New Contract: Execute a Job with initial data
    fun executeJob(jobId: String, data: Map<String, Any>)

    // 🟢 NEW: Tell the engine to wake up from a PENDING_REVIEW state
    fun resume(jobId: String)
}