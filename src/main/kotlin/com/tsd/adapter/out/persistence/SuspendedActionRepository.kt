package com.tsd.adapter.out.persistence

import com.tsd.core.model.SuspendedAction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SuspendedActionRepository : JpaRepository<SuspendedAction, Long> {

    // Helper to find the specific "Hold" for a specific task
    fun findByAccountIdAndCartridgeName(accountId: Long, cartridgeName: String): SuspendedAction?

    // Check if a hold exists (to prevent duplicate logging)
    fun existsByAccountIdAndCartridgeName(accountId: Long, cartridgeName: String): Boolean
}