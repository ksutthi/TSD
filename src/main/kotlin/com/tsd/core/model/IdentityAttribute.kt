package com.tsd.core.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "Identity_Attributes")
data class IdentityAttribute(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Attribute_ID")
    var attributeId: Long? = null,

    // 🟢 THE FIX: Make this nullable (Long? = null) and remove nullable = false
    @Column(name = "GIN_ID")
    var globalInvestorId: Long? = null,

    @Column(name = "Attribute_Type", nullable = false, length = 50)
    var attributeType: String,

    @Column(name = "Attribute_Value", nullable = false)
    var attributeValue: String,

    @Column(name = "Last_Updated")
    var lastUpdated: LocalDateTime = LocalDateTime.now(),

    @Column(name = "Update_Source", length = 50)
    var updateSource: String = "SYSTEM_RESOLUTION"
)