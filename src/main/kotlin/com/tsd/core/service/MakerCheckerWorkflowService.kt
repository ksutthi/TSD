package com.tsd.core.service

import com.tsd.adapter.output.persistence.CorporateActionJobRepository
import com.tsd.core.model.JobStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MakerCheckerWorkflowService(
    private val repository: CorporateActionJobRepository
    // private val workflowEngine: EnterpriseWorkflowEngine // We will inject your engine here later to resume jobs
) {
    private val log = LoggerFactory.getLogger(MakerCheckerWorkflowService::class.java)

    /**
     * Called by the Workflow Engine when it hits a step in the CSV matrix
     * that requires human intervention (e.g., "MANUAL_APPROVAL_REQUIRED").
     */
    @Transactional
    fun pauseForReview(jobId: String) {
        val job = repository.findByJobId(jobId)
            ?: throw IllegalArgumentException("Job $jobId not found in the vault.")

        job.status = JobStatus.PENDING_REVIEW
        repository.save(job)

        log.info("Job $jobId safely paused and routed to PENDING_REVIEW queue.")
    }

    /**
     * Called by the REST Controller when the Checker clicks "Approve" in the React UI.
     */
    @Transactional
    fun approveJob(jobId: String, checkerId: String) {
        val job = repository.findByJobId(jobId)
            ?: throw IllegalArgumentException("Job $jobId not found in the vault.")

        // 1. The Entity enforces the 10 Commandments (Maker != Checker)
        job.approve(checkerId)

        // 2. Commit to Database
        // 🔒 The exact moment Spring Boot checks the @Version. If it fails, it throws an exception here.
        repository.save(job)
        log.info("Job $jobId successfully APPROVED by $checkerId.")

        // 3. Wake the engine back up to continue the CSV matrix
        // workflowEngine.resume(job.jobId)
    }

    /**
     * Called by the REST Controller when the Checker clicks "Reject".
     */
    @Transactional
    fun rejectJob(jobId: String, checkerId: String) {
        val job = repository.findByJobId(jobId)
            ?: throw IllegalArgumentException("Job $jobId not found in the vault.")

        job.reject(checkerId)
        repository.save(job)

        log.warn("Job $jobId REJECTED by $checkerId. Workflow terminated.")
    }
}