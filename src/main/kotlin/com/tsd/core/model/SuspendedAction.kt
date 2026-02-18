package com.tsd.core.model

import com.tsd.adapter.output.persistence.JsonAttributeConverter
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "Suspended_Actions")
data class SuspendedAction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "Account_ID")
    val accountId: Long = 0,

    @Column(name = "Cartridge_Name")
    val cartridgeName: String = "",

    @Column(name = "Suspense_Code")
    val suspenseCode: String = "",

    @Column(name = "Reason")
    val reason: String = "",

    // PENDING, APPROVED, REJECTED
    @Column(name = "Status")
    var status: String = "PENDING",

    @Column(name = "Resolution_Type")
    var resolutionType: String = "RETRY",

    // 👇 ADD THIS: Store the transaction payload here!
    @Convert(converter = JsonAttributeConverter::class)
    @Column(name = "Context_Data", length = 4000)
    val contextData: Map<String, Any> = mapOf(),

    @Column(name = "Created_At")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "Updated_At")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)