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
    val stepId: Int, // Note: You have Int here, Engine treats it as part of ID string usually. Ideally String, but Int is fine.
    val cartridgeId: String,
    val cartridgeName: String,
    val strategy: String,
    val selectorLogic: String,

    // 🟢 NEW: Scope
    val scope: String,

    // 🟢 NEW: Configuration Injection (Required for Chaos Monkey)
    // We add a default value = "{}" so existing code doesn't break.
    val configJson: String = "{}"
)