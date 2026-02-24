package com.tsd.adapter.output.persistence

import com.tsd.core.model.AuditLog
import com.tsd.core.port.output.AuditLogPort
import org.springframework.stereotype.Component

@Component
class AuditRepositoryAdapter(private val jpaRepo: AuditRepository) : AuditLogPort {

    override fun save(log: AuditLog) {
        jpaRepo.save(log)
    }
}