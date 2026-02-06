package com.tsd.platform.persistence

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "\"Workflow_Audit\"") // Force "Workflow_Audit" case
data class AuditLog(

    // 1) ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 2) Module (e.g. "Module J")
    @Column(name = "Module")
    val module: String,

    // 3) Slot (e.g. "J1")
    @Column(name = "Slot")
    val slot: String,

    // 4) Step (e.g. "J1.1")
    @Column(name = "Step")
    val stepCode: String,

    // 5) Strategy (e.g. "Serial")
    @Column(name = "Strategy")
    val strategy: String,

    // 6) Cartridge Name
    @Column(name = "Cartridge_Name")
    val cartridge: String,

    // 7) Status (e.g. "SUCCESS")
    @Column(name = "Status")
    val status: String,

    // 8) Timestamp
    @Column(name = "Timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    // 9) Trace_ID
    @Column(name = "Trace_ID")
    val traceId: String,

    // (Hidden 10th) Message - We still need this to store error details!
    @Column(name = "Message", length = 4000)
    val message: String
)