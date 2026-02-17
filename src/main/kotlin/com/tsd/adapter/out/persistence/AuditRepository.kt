package com.tsd.adapter.out.persistence

import com.tsd.core.model.AuditLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuditRepository : JpaRepository<AuditLog, Long>