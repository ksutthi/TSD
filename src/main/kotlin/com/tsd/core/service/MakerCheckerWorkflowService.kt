package com.tsd.core.service

import com.tsd.adapter.output.persistence.CorporateActionJobRepository
import com.tsd.core.model.JobStatus
import com.tsd.core.model.PlatformAction
import com.tsd.core.port.output.PolicyEnginePort
import com.tsd.core.port.output.SecurityContextPort
import com.tsd.platform.spi.WorkflowEngine // 🟢 Imported the Engine Interface!
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class MakerCheckerWorkflowService(
    private val repository: CorporateActionJobRepository,
    private val securityPort: SecurityContextPort, // 🛡️ INJECTED
    private val policyPort: PolicyEnginePort,      // ⚖️ INJECTED
    private val workflowEngine: WorkflowEngine     // 🟢 UNCOMMENTED & INJECTED!
) {
    private val log = LoggerFactory.getLogger(MakerCheckerWorkflowService::class.java)

    @Transactional
    fun pauseForReview(jobId: String) {
        val job = repository.findByJobId(jobId)
            ?: throw IllegalArgumentException("Job $jobId not found in the vault.")

        job.status = JobStatus.PENDING_REVIEW
        repository.save(job)

        log.info("Job $jobId safely paused and routed to PENDING_REVIEW queue.")
    }

    /**
     * Upgraded: No longer accepts 'checkerId' from the outside.
     * The engine securely extracts it from the current thread.
     */
    @Transactional
    fun approveJob(jobId: String) {
        // 1. Fetch Job
        val job = repository.findByJobId(jobId)
            ?: throw IllegalArgumentException("Job $jobId not found in the vault.")

        // 2. Extract Trusted Identity
        val currentUser = securityPort.getCurrentUser()
        log.info("⚙️ [CORE ENGINE] ${currentUser.fullName} (${currentUser.role}) is attempting to approve Job $jobId")

        // 3. Enforce Policy (Assuming job has an amount, or defaulting to ZERO if not applicable)
        // NOTE: If your Job entity has a specific amount field (e.g., job.totalValue), replace BigDecimal.ZERO with it!
        val isAuthorized = policyPort.isAuthorized(currentUser, PlatformAction.APPROVE_TRANSFER, BigDecimal.ZERO)
        if (!isAuthorized) {
            log.error("🚨 SECURITY HARD-STOP: User ${currentUser.userId} lacks authority to approve this job!")
            throw SecurityException("Access Denied: Insufficient authority to approve Job $jobId.")
        }

        // 4. Domain Logic (Entity enforces Maker != Checker)
        job.approve(currentUser.userId)

        // 5. Commit to Database
        repository.save(job)
        log.info("✅ Job $jobId successfully APPROVED by secure identity: ${currentUser.userId}.")

        // 🟢 UNCOMMENTED: Wake up the Engine!
        workflowEngine.resume(jobId)
    }

    /**
     * Upgraded: Securely extracts rejecter identity.
     */
    @Transactional
    fun rejectJob(jobId: String) {
        val job = repository.findByJobId(jobId)
            ?: throw IllegalArgumentException("Job $jobId not found in the vault.")

        val currentUser = securityPort.getCurrentUser()

        // Checkers and Admins can reject. We use the same approval permission check for simplicity here.
        val isAuthorized = policyPort.isAuthorized(currentUser, PlatformAction.APPROVE_TRANSFER, BigDecimal.ZERO)
        if (!isAuthorized) {
            throw SecurityException("Access Denied: Insufficient authority to reject Job $jobId.")
        }

        job.reject(currentUser.userId)
        repository.save(job)

        log.warn("❌ Job $jobId REJECTED by ${currentUser.userId}. Workflow terminated.")
    }
}