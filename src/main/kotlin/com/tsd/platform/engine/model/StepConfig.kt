package com.tsd.platform.engine.model

/**
 * Metadata for the Orchestrator.
 * This defines one "Step" in a registrar's business process.
 * By changing these objects in the DB, you can reconfigure a tenant's
 * business logic without a single line of code change.
 */
data class StepConfig(
    val stepId: String,          // Unique ID for this step in the workflow
    val cartridgeClass: String,  // The Class Name (e.g., "TaxCartridge")
    val module: String,          // The A-Z Path (e.g., "distribution.taxation")
    val executionMode: String,   // SEQUENTIAL or PARALLEL
    val condition: String,       // Logic to decide if this step runs (e.g., "Region==TH" or "ALL")
    val inputMapping: String,    // Optional mapping for data transformation
    val outputMapping: String,   // Optional mapping for data transformation
    val parameters: Map<String, String> // Module variables (e.g., "RATE" -> "0.15")
) {
    /**
     * Helper to check if the condition matches the current context data.
     * Expects condition format: "Key==Value" or "ALL"
     */
    fun shouldExecute(contextData: Map<String, Any>): Boolean {
        if (condition.uppercase() == "ALL" || condition.isBlank()) return true

        val parts = condition.split("==")
        if (parts.size != 2) return true // Default to run if format is wrong to avoid silent skips

        val key = parts[0].trim()
        val expectedValue = parts[1].trim()
        val actualValue = contextData[key]?.toString() ?: ""

        return actualValue == expectedValue
    }
}