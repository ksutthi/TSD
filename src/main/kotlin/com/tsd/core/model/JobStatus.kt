package com.tsd.core.model

enum class JobStatus {
    INIT,
    RUNNING,
    PENDING_REVIEW,   // Job is paused, waiting for Checker
    APPROVED,         // Checker authorized, ready to resume
    REJECTED,         // Checker denied
    COMPLETED,
    FAILED
}