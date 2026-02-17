package com.tsd.platform.config

import com.tsd.platform.engine.model.ExecutionBlock
import com.tsd.platform.engine.model.MatrixRule
import org.springframework.stereotype.Component

@Component
class ConfigMatrix {

    // 🟢 Start with an empty list.
    // The WorkflowLoader will inject the data here.
    private var rules = mutableListOf<MatrixRule>()

    // 🟢 NEW: This is the bridge where WorkflowLoader passes the data
    fun loadRules(newRules: List<MatrixRule>) {
        this.rules.clear()
        this.rules.addAll(newRules)
        println("   🧠 [ConfigMatrix] Execution Plan Updated: ${rules.size} rules ready.")
    }

    // 🧠 THE SMART GROUPER (Your logic - Preserved!)
    fun getExecutionPlan(): List<ExecutionBlock> {
        val blocks = mutableListOf<ExecutionBlock>()
        if (rules.isEmpty()) return blocks

        var currentRules = mutableListOf<MatrixRule>()
        var currentScope = rules[0].scope
        var currentModule = rules[0].moduleId
        var blockIndex = 0

        for (rule in rules) {
            // Break block if Module changes OR Scope changes
            if (rule.moduleId != currentModule || rule.scope != currentScope) {
                blocks.add(ExecutionBlock(
                    uniqueId = "${currentModule}_$blockIndex",
                    moduleId = currentModule,
                    scope = currentScope,
                    rules = currentRules.toList()
                ))
                currentRules = mutableListOf()

                // Reset or Increment index
                if (rule.moduleId != currentModule) blockIndex = 0 else blockIndex++

                currentScope = rule.scope
                currentModule = rule.moduleId
            }
            currentRules.add(rule)
        }

        // Add the final block
        if (currentRules.isNotEmpty()) {
            blocks.add(ExecutionBlock(
                uniqueId = "${currentModule}_$blockIndex",
                moduleId = currentModule,
                scope = currentScope,
                rules = currentRules.toList()
            ))
        }

        return blocks
    }

    fun getRulesForModule(moduleId: String) = rules.filter { it.moduleId == moduleId }
    fun getOrderedModules(): List<String> = rules.map { it.moduleId }.distinct()
}