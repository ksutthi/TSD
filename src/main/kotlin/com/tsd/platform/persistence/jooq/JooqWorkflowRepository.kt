package com.tsd.platform.persistence.jooq

import com.tsd.platform.persistence.WorkflowRepository
import com.tsd.platform.persistence.jooq.schema.tables.references.WORKFLOW_JOB
import jakarta.annotation.PostConstruct
import org.jooq.DSLContext
import org.jooq.conf.RenderNameCase
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class JooqWorkflowRepository(
    private val dsl: DSLContext
) : WorkflowRepository {

    @PostConstruct
    fun init() {
        // 1. Force "Simple SQL" mode
        // This strips quotes and schema names to make SQL Server happy on the Free Tier.
        dsl.configuration().settings()
            .withRenderSchema(false)
            .withRenderNameCase(RenderNameCase.AS_IS)
            .withRenderQuotedNames(org.jooq.conf.RenderQuotedNames.NEVER)

        println("      🐘 [jOOQ] Repository Initialized (Universal Mode)")
    }

    override fun save(jobId: String, workflowId: String, currentStep: String, status: String, payloadJson: String) {

        // 🛡️ THE UNIVERSAL UPSERT (Works on ALL DBs)

        // 1. Try to UPDATE first
        val updatedRows = dsl.update(WORKFLOW_JOB)
            .set(WORKFLOW_JOB.CURRENT_STEP, currentStep)
            .set(WORKFLOW_JOB.STATUS, status)
            .set(WORKFLOW_JOB.PAYLOAD, payloadJson)
            .set(WORKFLOW_JOB.UPDATED_AT, java.time.LocalDateTime.now())
            .where(WORKFLOW_JOB.JOB_ID.eq(jobId))
            .execute()

        // 2. If 0 rows were updated, it means the job doesn't exist -> INSERT
        if (updatedRows == 0) {
            try {
                dsl.insertInto(WORKFLOW_JOB)
                    .set(WORKFLOW_JOB.JOB_ID, jobId)
                    .set(WORKFLOW_JOB.WORKFLOW_ID, workflowId)
                    .set(WORKFLOW_JOB.CURRENT_STEP, currentStep)
                    .set(WORKFLOW_JOB.STATUS, status)
                    .set(WORKFLOW_JOB.PAYLOAD, payloadJson)
                    .set(WORKFLOW_JOB.CREATED_AT, java.time.LocalDateTime.now())
                    .set(WORKFLOW_JOB.UPDATED_AT, java.time.LocalDateTime.now())
                    .execute()
            } catch (e: Exception) {
                // Edge Case: If two threads insert at the EXACT same time,
                // the second one fails. We catch that and update instead.
                dsl.update(WORKFLOW_JOB)
                    .set(WORKFLOW_JOB.CURRENT_STEP, currentStep)
                    .set(WORKFLOW_JOB.STATUS, status)
                    .set(WORKFLOW_JOB.PAYLOAD, payloadJson)
                    .where(WORKFLOW_JOB.JOB_ID.eq(jobId))
                    .execute()
            }
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
            .where(WORKFLOW_JOB.STATUS.eq("PAUSED"))
            .or(WORKFLOW_JOB.STATUS.eq("RUNNING"))
            .fetchInto(String::class.java)
    }
}