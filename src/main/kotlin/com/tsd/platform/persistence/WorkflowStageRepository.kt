package com.tsd.platform.persistence

import com.tsd.platform.model.registry.WorkflowStage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkflowStageRepository : JpaRepository<WorkflowStage, Long> {

    // Used by the Batch Reader to find items ready for the next stage
    fun findByCurrentStep(currentStep: String, pageable: Pageable): Page<WorkflowStage>
}