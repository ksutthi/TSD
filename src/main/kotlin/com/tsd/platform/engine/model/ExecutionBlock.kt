package com.tsd.platform.engine.model

// 📦 The Execution Unit (A group of rules running in the same scope)
data class ExecutionBlock(
    val uniqueId: String, // e.g. "M_0", "M_1"
    val moduleId: String,
    val scope: String,
    val rules: List<MatrixRule>
)