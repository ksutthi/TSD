package com.tsd.adapter.input.web

import com.tsd.adapter.output.connectivity.ExternalBankAdapter
import com.tsd.adapter.output.persistence.CorporateActionJobRepository
import com.tsd.core.model.CorporateActionJob
import com.tsd.core.service.MakerCheckerWorkflowService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/test")
class PlatformTestController(
    private val externalBankAdapter: ExternalBankAdapter,
    private val makerCheckerService: MakerCheckerWorkflowService,
    private val jobRepository: CorporateActionJobRepository
) {

    // ==========================================
    // TEST 1: IDEMPOTENCY (The Double-Click Test)
    // ==========================================
    @PostMapping("/idempotency")
    fun testIdempotency(): ResponseEntity<String> {
        Thread.sleep(2000)
        return ResponseEntity.ok("SUCCESS: Business logic executed once.")
    }

    // ==========================================
    // TEST 2: MAKER-CHECKER (The Vault Lock Test)
    // ==========================================
    @PostMapping("/maker-checker/init")
    fun initJob(): ResponseEntity<String> {
        val job = CorporateActionJob(
            jobId = "TEST-JOB-${UUID.randomUUID().toString().take(5)}",
            transactionType = "DIVIDEND_PAYOUT",
            currentState = "WAITING_APPROVAL",
            makerId = "ADMIN_MAKER_01",
            status = com.tsd.core.model.JobStatus.PENDING_REVIEW
        )
        jobRepository.save(job)
        return ResponseEntity.ok("Created Job: ${job.jobId} at Version 0")
    }

    // 🛡️ SECURITY FIX: Removed '@RequestParam checkerId'. The engine extracts it automatically!
    @PostMapping("/maker-checker/approve/{jobId}")
    fun approveJob(@PathVariable jobId: String): ResponseEntity<String> {
        makerCheckerService.approveJob(jobId) // ✅ Now only passes the jobId
        val updatedJob = jobRepository.findByJobId(jobId)
        return ResponseEntity.ok("Approved! New Database Version is: ${updatedJob?.version}")
    }

    // ==========================================
    // TEST 3: CIRCUIT BREAKER (The Outage Test)
    // ==========================================
    @GetMapping("/circuit-breaker")
    fun testCircuitBreaker(@RequestParam fail: Boolean): ResponseEntity<String> {
        val result = externalBankAdapter.initiateTransfer("ACCT_123", 5000.0, fail)
        return ResponseEntity.ok("Result: $result")
    }

    @GetMapping("/crash")
    fun crash(): String {
        throw RuntimeException("Simulated catastrophic database or memory failure")
    }
}



