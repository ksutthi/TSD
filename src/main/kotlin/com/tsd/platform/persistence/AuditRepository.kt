package com.tsd.platform.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

// This interface gives us .save(), .findAll(), .delete() for free!
@Repository
interface AuditRepository : JpaRepository<AuditLog, Long>