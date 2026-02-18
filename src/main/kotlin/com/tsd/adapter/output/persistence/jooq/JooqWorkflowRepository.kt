package com.tsd.adapter.output.persistence.jooq

import com.tsd.adapter.out.persistence.jooq.schema.tables.WorkflowJob.Companion.WORKFLOW_JOB
import com.tsd.adapter.output.persistence.WorkflowRepository
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JooqWorkflowRepository(
    private val dsl: DSLContext
) : WorkflowRepository {

    init {
        // 🚨 CRITICAL FIX: Tell jOOQ to NEVER print "PUBLIC" (Fixes SQL Server issues)
        dsl.configuration().settings().isRenderSchema = false
    }

    override fun save(jobId: String, workflowId: String, currentStep: String, status: String, payloadJson: String) {
        val count = dsl.selectCount()
            .from(WORKFLOW_JOB)
            .where(WORKFLOW_JOB.JOB_ID.eq(jobId))
            .fetchOne(0, Int::class.java) ?: 0

        if (count > 0) {
            dsl.update(WORKFLOW_JOB)
                .set(WORKFLOW_JOB.STATUS, status)
                .set(WORKFLOW_JOB.CURRENT_STEP, currentStep)
                .set(WORKFLOW_JOB.PAYLOAD, payloadJson)
                .where(WORKFLOW_JOB.JOB_ID.eq(jobId))
                .execute()
        } else {
            dsl.insertInto(WORKFLOW_JOB)
                .set(WORKFLOW_JOB.JOB_ID, jobId)
                .set(WORKFLOW_JOB.WORKFLOW_ID, workflowId)
                .set(WORKFLOW_JOB.CURRENT_STEP, currentStep)
                .set(WORKFLOW_JOB.STATUS, status)
                .set(WORKFLOW_JOB.PAYLOAD, payloadJson)
                .execute()
        }
    }

    override fun findStatus(jobId: String): String? {
        return dsl.select(WORKFLOW_JOB.STATUS)
            .from(WORKFLOW_JOB)
            .where(WORKFLOW_JOB.JOB_ID.eq(jobId))
            .fetchOneInto(String::class.java)
    }

    override fun findPayload(jobId: String): String? {
        return dsl.select(WORKFLOW_JOB.PAYLOAD)
            .from(WORKFLOW_JOB)
            .where(WORKFLOW_JOB.JOB_ID.eq(jobId))
            .fetchOneInto(String::class.java)
    }

    override fun findStuckJobs(): List<String> {
        return dsl.select(WORKFLOW_JOB.JOB_ID)
            .from(WORKFLOW_JOB)
            .where(WORKFLOW_JOB.STATUS.eq("RUNNING"))
            .fetchInto(String::class.java)
    }

    override fun existsByWorkflowId(workflowId: String): Boolean {
        val count = dsl.selectCount()
            .from(WORKFLOW_JOB)
            .where(WORKFLOW_JOB.WORKFLOW_ID.eq(workflowId))
            .and(WORKFLOW_JOB.STATUS.ne("FAILED"))
            .fetchOne(0, Int::class.java) ?: 0

        return count > 0
    }

    // 🟢 NEW: Refactored logic from Engine
    override fun isStepAlreadyDone(jobId: String, stepCode: String): Boolean {
        // Using raw SQL via jOOQ since we haven't generated the Workflow_Audit jOOQ table yet.
        // This is safe and robust.
        val sql = "SELECT COUNT(*) FROM Workflow_Audit WHERE Trace_ID = ? AND Step = ? AND Status = 'CLEARED'"
        val result = dsl.fetchOne(sql, jobId, stepCode)
        return (result?.get(0, Int::class.java) ?: 0) > 0
    }
}