package com.tsd.adapter.output.persistence

import com.tsd.core.model.CorporateActionJob
import com.tsd.core.model.JobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CorporateActionJobRepository : JpaRepository<CorporateActionJob, UUID> {
    fun findByJobId(jobId: String): CorporateActionJob?

    // Used by the Lazarus Protocol to find jobs that were interrupted
    fun findByStatus(status: JobStatus): List<CorporateActionJob>
}