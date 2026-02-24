package com.tsd.core.port.output

import com.tsd.core.model.AuditLog

interface AuditLogPort {
    fun save(log: AuditLog)
}