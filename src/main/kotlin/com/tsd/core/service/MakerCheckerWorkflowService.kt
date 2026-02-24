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
     * Upgraded: Matrix-Engine Compatible.
     * Enforces strict security policies, updates legacy tables if they exist,
     * and seamlessly wakes up the modern Workflow Engine.
     */
    @Transactional
    fun approveJob(jobId: String) {
        // 1. Extract Trusted Identity FIRST
        val currentUser = securityPort.getCurrentUser()
        log.info("⚙️ [CORE ENGINE] ${currentUser.fullName} (${currentUser.role}) is attempting to approve Job $jobId")

        // 2. Enforce Policy (Regardless of which engine owns the job)
        val isAuthorized = policyPort.isAuthorized(currentUser, PlatformAction.APPROVE_TRANSFER, BigDecimal.ZERO)
        if (!isAuthorized) {
            log.error("🚨 SECURITY HARD-STOP: User ${currentUser.userId} lacks authority to approve this job!")
            throw SecurityException("Access Denied: Insufficient authority to approve Job $jobId.")
        }

        // 3. Try to update the Legacy Vault (if the entity exists)
        val job = repository.findByJobId(jobId)
        if (job != null) {
            // Domain Logic (Entity enforces Maker != Checker)
            job.approve(currentUser.userId)
            repository.save(job)
            log.info("✅ Legacy Job $jobId successfully APPROVED by secure identity: ${currentUser.userId}.")
        } else {
            // Matrix Engine Jobs might not use the legacy entity, so we just log and proceed
            log.info("💡 [Matrix Handoff] Job $jobId not found in legacy vault. Trusting Matrix Engine state.")
        }

        // 4. ALWAYS Wake up the Core Matrix Engine!
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