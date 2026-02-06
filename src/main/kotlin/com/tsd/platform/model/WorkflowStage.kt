package com.tsd.platform.model

import jakarta.persistence.*
import com.tsd.platform.persistence.JsonAttributeConverter // 👈 FIXED: Importing the real class

@Entity
@Table(name = "Workflow_Stage")
data class WorkflowStage(
    @Id
    @Column(name = "Account_ID")
    val accountId: Long = 0,

    @Column(name = "Current_Step")
    val currentStep: String = "",

    // 👇 FIXED: Using JsonAttributeConverter instead of MapToJsonConverter
    @Convert(converter = JsonAttributeConverter::class)
    @Column(name = "Context_Data", length = 4000)
    val contextData: Map<String, Any> = mapOf()
)