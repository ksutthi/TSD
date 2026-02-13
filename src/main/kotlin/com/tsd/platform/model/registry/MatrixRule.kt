package com.tsd.platform.model.registry

data class MatrixRule(
    // 🟢 NEW: Multi-Tenancy Keys
    val registrarCode: String,
    val workflowId: String,

    // Existing Fields
    val moduleId: String,
    val moduleName: String,
    val slotId: String,
    val slotName: String,
    val stepId: Int,
    val cartridgeId: String,
    val cartridgeName: String,
    val strategy: String,
    val selectorLogic: String,

    // 🟢 NEW: Scope
    val scope: String
)