package com.tsd.platform.engine.model

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
    val scope: String,

    // 🟢 NEW: Saga Control
    // If FALSE, the engine will NOT call compensate() on failure.
    // Useful for "Log to Console" or "Send Notification" steps that cannot be undone.
    val isCompensatable: Boolean = true, // Default to TRUE (Safety First)

    // 🟢 NEW: Configuration Injection (Required for Chaos Monkey)
    // We add a default value = "{}" so existing code doesn't break.
    val configJson: String = "{}"
)