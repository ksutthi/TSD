package com.tsd.platform.model

data class WorkflowRule(
    val moduleId: String,
    val moduleName: String,
    val slotId: String,
    val slotName: String,
    val stepId: Int,          // Engine expects Int, not String
    val cartridgeId: String,
    val cartridgeName: String,
    val strategy: String,
    val selectorLogic: String // Engine expects "selectorLogic", not "selector"
)