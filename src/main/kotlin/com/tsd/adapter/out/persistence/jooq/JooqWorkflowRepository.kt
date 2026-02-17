package com.tsd.adapter.out.persistence.jooq

import com.tsd.adapter.out.persistence.WorkflowRepository
import com.tsd.adapter.out.persistence.jooq.schema.tables.WorkflowJob
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JooqWorkflowRepository(
    private val dsl: DSLContext
) : WorkflowRepository {

    init {
        // 🚨 CRITICAL FIX: Tell jOOQ to NEVER print "PUBLIC" in the SQL.
        // This fixes the "Invalid object name" error on SQL Server.
        dsl.configuration().settings().isRenderSchema = false
    }

    override fun save(jobId: String, workflowId: String, currentStep: String, status: String, payloadJson: String) {

        // 1. Check if job exists (Safe Count Method)
        val count = dsl.selectCount()
            .from(WorkflowJob.WORKFLOW_JOB)
            .where(WorkflowJob.WORKFLOW_JOB.JOB_ID.eq(jobId))
            .fetchOne(0, Int::class.java) ?: 0

        if (count > 0) {
            // 2. UPDATE existing
            dsl.update(WorkflowJob.WORKFLOW_JOB)
                .set(WorkflowJob.WORKFLOW_JOB.STATUS, status)
                .set(WorkflowJob.WORKFLOW_JOB.CURRENT_STEP, currentStep)
                .set(WorkflowJob.WORKFLOW_JOB.PAYLOAD, payloadJson)
                .where(WorkflowJob.WORKFLOW_JOB.JOB_ID.eq(jobId))
                .execute()
        } else {
            // 3. INSERT new
            dsl.insertInto(WorkflowJob.WORKFLOW_JOB)
                .set(WorkflowJob.WORKFLOW_JOB.JOB_ID, jobId)
                .set(WorkflowJob.WORKFLOW_JOB.WORKFLOW_ID, workflowId)
                .set(WorkflowJob.WORKFLOW_JOB.CURRENT_STEP, currentStep)
                .set(WorkflowJob.WORKFLOW_JOB.STATUS, status)
                .set(WorkflowJob.WORKFLOW_JOB.PAYLOAD, payloadJson)
                .execute()
        }
    }

    override fun findStatus(jobId: String): String? {
        return dsl.select(WorkflowJob.WORKFLOW_JOB.STATUS)
            .from(WorkflowJob.WORKFLOW_JOB)
            .where(WorkflowJob.WORKFLOW_JOB.JOB_ID.eq(jobId))
            .fetchOneInto(String::class.java)
    }

    override fun findPayload(jobId: String): String? {
        return dsl.select(WorkflowJob.WORKFLOW_JOB.PAYLOAD)
            .from(WorkflowJob.WORKFLOW_JOB)
            .where(WorkflowJob.WORKFLOW_JOB.JOB_ID.eq(jobId))
            .fetchOneInto(String::class.java)
    }

    override fun findStuckJobs(): List<String> {
        return dsl.select(WorkflowJob.WORKFLOW_JOB.JOB_ID)
            .from(WorkflowJob.WORKFLOW_JOB)
            .where(WorkflowJob.WORKFLOW_JOB.STATUS.eq("RUNNING"))
            .fetchInto(String::class.java)
    }
}