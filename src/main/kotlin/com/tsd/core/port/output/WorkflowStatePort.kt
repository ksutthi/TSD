package com.tsd.core.port.output

interface WorkflowStatePort {
    fun save(jobId: String, workflowId: String, step: String, status: String, payload: String)
    fun isStepAlreadyDone(jobId: String, stepCode: String): Boolean
}