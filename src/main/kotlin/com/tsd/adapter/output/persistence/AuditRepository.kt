package com.tsd.adapter.output.persistence

// 🟢 FIX: Import from the CORE model, not the deleted 'features' package
import com.tsd.core.model.AuditLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuditRepository : JpaRepository<AuditLog, Long>