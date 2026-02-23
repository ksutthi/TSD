package com.tsd.core.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "CORPORATE_ACTION_JOB")
class CorporateActionJob(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "job_id", unique = true, nullable = false)
    val jobId: String,

    @Column(name = "transaction_type", nullable = false)
    val transactionType: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: JobStatus = JobStatus.INIT,

    @Column(name = "current_state", nullable = false)
    var currentState: String, // Tracks the exact step in the CSV matrix (e.g., "CALC_WHT")

    @Column(name = "maker_id", nullable = false)
    val makerId: String,

    @Column(name = "checker_id")
    var checkerId: String? = null,

    // 🔒 THE VAULT LOCK (Optimistic Locking)
    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    // Business Logic Encapsulation: The Entity protects its own state
    fun approve(approvedBy: String) {
        if (this.makerId == approvedBy) {
            throw IllegalArgumentException("Maker cannot be the Checker. Segregation of duties violated.")
        }
        if (this.status != JobStatus.PENDING_REVIEW) {
            throw IllegalStateException("Job must be in PENDING_REVIEW to be approved. Current status: ${this.status}")
        }
        this.status = JobStatus.APPROVED
        this.checkerId = approvedBy
    }

    fun reject(rejectedBy: String) {
        if (this.status != JobStatus.PENDING_REVIEW) {
            throw IllegalStateException("Job must be in PENDING_REVIEW to be rejected. Current status: ${this.status}")
        }
        this.status = JobStatus.REJECTED
        this.checkerId = rejectedBy
    }
}