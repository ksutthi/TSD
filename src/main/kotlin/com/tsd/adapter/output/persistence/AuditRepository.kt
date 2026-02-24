package com.tsd.adapter.output.persistence

import com.tsd.core.model.AuditLog
import com.tsd.core.port.output.AuditLogPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Component

// 1. Your existing Spring Data Repository (Handles the actual database work)
@Repository
interface AuditRepository : JpaRepository<AuditLog, Long>

