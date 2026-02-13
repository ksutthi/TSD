package com.tsd.platform.persistence.jooq

import com.tsd.platform.persistence.WorkflowRepository
// 🟢 IMPORT THE GENERATED TABLE (Check your build/generated folder if this is red)
import com.tsd.platform.persistence.jooq.schema.tables.references.WORKFLOW_JOB
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JooqWorkflowRepository(
    private val dsl: DSLContext // 🔌 Inject jOOQ
) : WorkflowRepository {

    override fun save(jobId: String, workflowId: String, currentStep: String, status: String, payloadJson: String) {

        // 🛡️ TYPE-SAFE SQL!
        // If you rename a DB column, this code stops compiling.
        dsl.insertInto(WORKFLOW_JOB)
            .set(WORKFLOW_JOB.JOB_ID, jobId)
            .set(WORKFLOW_JOB.WORKFLOW_ID, workflowId)
            .set(WORKFLOW_JOB.CURRENT_STEP, currentStep)
            .set(WORKFLOW_JOB.STATUS, status)
            .set(WORKFLOW_JOB.PAYLOAD, payloadJson)
            .onDuplicateKeyUpdate() // Upsert (Update if exists)
            .set(WORKFLOW_JOB.CURRENT_STEP, currentStep)
            .set(WORKFLOW_JOB.STATUS, status)
            .set(WORKFLOW_JOB.PAYLOAD, payloadJson)
            .set(WORKFLOW_JOB.UPDATED_AT, java.time.LocalDateTime.now())
            .execute()
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
            .or(WORKFLOW_JOB.STATUS.eq("RUNNING")) // Crashed while running
            .fetchInto(String::class.java)
    }
}