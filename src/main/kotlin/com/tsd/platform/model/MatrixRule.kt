package com.tsd.platform.model

// 🧱 The Atomic Rule from CSV
data class MatrixRule(
    val moduleId: String,
    val moduleName: String,
    val slotId: String,
    val slotName: String,
    val stepId: String,
    val cartridgeId: String,
    val cartridgeName: String,
    val strategy: String,
    val selector: String,
    val scope: String
)