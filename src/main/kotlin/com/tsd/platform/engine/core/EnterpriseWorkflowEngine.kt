package com.tsd.platform.engine.core

import com.tsd.platform.engine.loader.WorkflowLoader
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.model.registry.MatrixRule
import com.tsd.platform.persistence.AuditLog
import com.tsd.platform.persistence.AuditRepository
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.WorkflowEngine
import com.tsd.platform.spi.Cartridge
import kotlinx.coroutines.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class EnterpriseWorkflowEngine(
    private val workflowLoader: WorkflowLoader,
    private val existingCartridges: List<Cartridge>,
    private val auditRepo: AuditRepository
) : WorkflowEngine {

    // ⚡ Fast Lookup Map: "CartridgeID" -> Instance
    private val cartridgeMap = existingCartridges.associateBy { it.id }

    override fun executeJob(jobId: String, data: Map<String, Any>) {
        println(EngineAnsi.CYAN + "🚀 [Engine-Core] Starting Job: $jobId" + EngineAnsi.RESET)

        // 1. Inflate Packet
        val packet = ExchangePacket(id = jobId, traceId = jobId)
        packet.data.putAll(data)

        // 2. Create Context & Populate
        val context = KernelContext(jobId, "DEFAULT")
        data.forEach { (k, v) -> context.set(k, v) }

        // 3. Run Strategies
        runWorkflow(packet, context)
    }

    private fun runWorkflow(packet: ExchangePacket, context: KernelContext) {
        val allRules = workflowLoader.rules

        // 🟢 FIX: Use getObject instead of get
        val targetModule = context.getObject<String>("Workflow_ID")

        val activeRules = if (targetModule != null) {
            allRules.filter { it.moduleId == targetModule }
        } else {
            allRules // Fallback: Run everything (Legacy mode)
        }

        if (activeRules.isEmpty()) {
            println(EngineAnsi.RED + "   ⚠️ No rules found for Workflow_ID: $targetModule" + EngineAnsi.RESET)
            return
        }

        // Group by Unique Step ID
        val stepGroups = activeRules.groupBy { "${it.moduleId}-${it.slotId}-${it.stepId}" }

        stepGroups.forEach { (_, rulesInStep) ->
            // Assume strategy is consistent across the step group
            val strategy = rulesInStep.first().strategy.uppercase()

            when (strategy) {
                "PARALLEL"  -> executeParallel(rulesInStep, packet, context)
                "CONSENSUS" -> executeConsensus(rulesInStep, packet, context)
                "ASYNC"     -> executeAsync(rulesInStep, packet, context)
                "RETRY"     -> executeWithRetry(rulesInStep, packet, context)
                else        -> executeSerial(rulesInStep, packet, context)
            }
        }

        println(EngineAnsi.CYAN + "🏁 [Engine-Core] Job ${context.jobId} Complete." + EngineAnsi.RESET)
    }

    // --- STRATEGIES ---

    private fun executeSerial(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        rules.forEach { rule ->
            if (shouldExecute(rule, packet)) runCartridge(rule, packet, context)
        }
    }

    private fun executeParallel(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val activeRules = rules.filter { shouldExecute(it, packet) }
        if (activeRules.isEmpty()) return

        println("      " + EngineAnsi.CYAN + "⚡ [Parallel] Forking ${activeRules.size} tasks..." + EngineAnsi.RESET)

        runBlocking {
            val jobs = activeRules.map { rule ->
                async(Dispatchers.Default) {
                    runCartridge(rule, packet, context)
                }
            }
            jobs.awaitAll()
        }
    }

    private fun executeConsensus(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        // Run logic normally; the Cartridge itself calls consensusService.waitForConsensus()
        executeSerial(rules, packet, context)
    }

    private fun executeAsync(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val activeRules = rules.filter { shouldExecute(it, packet) }
        if (activeRules.isEmpty()) return

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            activeRules.forEach { rule ->
                try {
                    runCartridge(rule, packet, context)
                } catch (e: Exception) {
                    println(EngineAnsi.RED + "   ⚠️ Async Task Failed: ${e.message}" + EngineAnsi.RESET)
                }
            }
        }
    }

    private fun executeWithRetry(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        rules.filter { shouldExecute(it, packet) }.forEach { rule ->
            try {
                runCartridge(rule, packet, context)
            } catch (e: Exception) {
                println(EngineAnsi.RED + "      🔄 [Retry] Failed. Retrying..." + EngineAnsi.RESET)
            }
        }
    }

    // --- 🟢 LOGIC ENGINE ---

    private fun shouldExecute(rule: MatrixRule, packet: ExchangePacket): Boolean {
        val logic = rule.selectorLogic

        // 1. Simple Case
        if (logic.isBlank() || logic == "*") return true

        // 2. Parse "KEY == VALUE"
        if (logic.contains("==")) {
            val parts = logic.split("==")
            val key = parts[0].trim()
            val expectedValue = parts[1].trim().replace("'", "")

            // Look in Packet Data
            val actualValue = packet.data[key]?.toString()
                ?: if (key == "Event_Type") packet.data["Event_Type"]?.toString() else null

            return actualValue == expectedValue
        }

        // 3. Parse "KEY > VALUE"
        if (logic.contains(">")) {
            val parts = logic.split(">")
            val key = parts[0].trim()
            val threshold = parts[1].trim().toDoubleOrNull() ?: 0.0

            val actualValue = packet.data[key]?.toString()?.toDoubleOrNull() ?: 0.0
            return actualValue > threshold
        }

        return false
    }

    private fun runCartridge(rule: MatrixRule, packet: ExchangePacket, context: KernelContext) {
        val cartridge = cartridgeMap[rule.cartridgeId]
        val stepCode = "[${rule.moduleId}-${rule.slotId}]"

        if (cartridge != null) {
            try {
                context.set("STEP_PREFIX", stepCode)
                cartridge.execute(packet, context)
                logToDb(packet.id, rule, stepCode, "SUCCESS", "Executed")
            } catch (e: Exception) {
                println(EngineAnsi.RED + "      💥 Error in ${rule.cartridgeId}: ${e.message}" + EngineAnsi.RESET)
                logToDb(packet.id, rule, stepCode, "FAILED", e.message ?: "Error")
                throw e
            }
        } else {
            println(EngineAnsi.RED + "      ⚠️ Cartridge Not Found: ${rule.cartridgeId}" + EngineAnsi.RESET)
        }
    }

    private fun logToDb(traceId: String, rule: MatrixRule, stepCode: String, status: String, msg: String) {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val log = AuditLog(
                    traceId = traceId,
                    module = rule.moduleId,
                    slot = rule.slotId,
                    stepCode = stepCode,
                    strategy = rule.strategy,
                    cartridge = rule.cartridgeId,
                    status = status,
                    message = msg
                )
                auditRepo.save(log)
            } catch (e: Exception) { }
        }
    }
}