package com.tsd.platform.model.registry

data class WorkflowRule(
    // 🟢 NEW: Multi-Tenancy Keys
    val registrarCode: String,
    val workflowId: String,

    // Existing Fields
    val moduleId: String,
    val moduleName: String,
    val slotId: String,
    val slotName: String,
    val stepId: Int,           // Engine expects Int
    val cartridgeId: String,
    val cartridgeName: String,
    val strategy: String,
    val selectorLogic: String, // Engine expects "selectorLogic"

    // 🟢 NEW: Scope (Job vs Item) - Required for Batch Engine
    val scope: String
)