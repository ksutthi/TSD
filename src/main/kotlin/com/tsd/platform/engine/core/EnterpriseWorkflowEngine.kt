package com.tsd.platform.engine.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.tsd.adapter.out.persistence.AuditRepository
import com.tsd.platform.engine.loader.WorkflowLoader
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.engine.model.MatrixRule
import com.tsd.core.model.AuditLog
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.WorkflowEngine
import kotlinx.coroutines.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import com.tsd.platform.engine.state.KernelContext

@Service
@Primary
class EnterpriseWorkflowEngine(
    private val workflowLoader: WorkflowLoader,
    private val existingCartridges: List<Cartridge>, // 🟢 Raw list containing duplicates
    private val auditRepo: AuditRepository,
    private val objectMapper: ObjectMapper
) : WorkflowEngine {

    // 🟢 INTELLIGENT LOADING: Filters duplicates and keeps only the highest version
    private val cartridgeMap: Map<String, Cartridge> = existingCartridges
        .groupBy { it.id }
        .mapValues { (id, versions) ->
            if (versions.size > 1) {
                // If multiple versions exist, pick the one with the highest version string
                val winner = versions.maxByOrNull { it.version }!!
                println(EngineAnsi.YELLOW + "   ⚠️ [Engine] Duplicate Cartridge ID '$id' detected. Using v${winner.version} (Ignored older versions)." + EngineAnsi.RESET)
                winner
            } else {
                versions.first()
            }
        }

    // 🟢 NEW HELPER METHOD: This fixes the "Unresolved reference" in TsdApplication
    fun executeWorkflow(registrar: String, workflowId: String) {
        val jobId = "JOB-${System.currentTimeMillis()}"
        println(EngineAnsi.CYAN + "🚀 [Engine-Entry] Manual Trigger: $workflowId ($registrar) -> Job: $jobId" + EngineAnsi.RESET)

        val contextData = mapOf(
            "Registrar_Code" to registrar,
            "Workflow_ID" to workflowId
        )

        // Delegate to the main execution method
        executeJob(jobId, contextData)
    }

    override fun executeJob(jobId: String, data: Map<String, Any>) {
        println(EngineAnsi.CYAN + "🚀 [Engine-Core] Starting Job: $jobId" + EngineAnsi.RESET)

        val packet = ExchangePacket(id = jobId, traceId = jobId)
        // 🟢 SAFETY: Copy map safely
        data.forEach { (k, v) -> packet.data[k] = v }

        val context = KernelContext(jobId, "DEFAULT")
        data.forEach { (k, v) -> context.set(k, v) }

        try {
            runWorkflow(packet, context)
        } catch (e: Exception) {
            println(EngineAnsi.RED + "💥 Job Failed: ${e.message}" + EngineAnsi.RESET)
            throw e
        }
    }

    private fun runWorkflow(packet: ExchangePacket, context: KernelContext) {
        val allRules = workflowLoader.rules

        // 🟢 FIX 1: Correct Lookup Logic (Workflow + Registrar)
        val workflowId = context.getObject<String>("Workflow_ID") ?: "UNKNOWN"
        val registrarCode = context.getObject<String>("Registrar_Code") ?: "TSD"

        // println("   🔍 Debug: Looking for Registrar='$registrarCode', Workflow='$workflowId'")

        // 🟢 FIX 2: Filter by WorkflowId, NOT ModuleId
        val activeRules = allRules.filter {
            it.registrarCode == registrarCode && it.workflowId == workflowId
        }

        if (activeRules.isEmpty()) {
            println(EngineAnsi.RED + "   ⚠️ No rules found for Registrar: $registrarCode, Workflow_ID: $workflowId" + EngineAnsi.RESET)
            return
        }

        // Group by Module -> Slot -> Step
        val stepGroups = activeRules.groupBy { "${it.moduleId}-${it.slotId}-${it.stepId}" }

        stepGroups.forEach { (stepCode, rulesInStep) ->
            // println("      ⚙️ Executing Step: $stepCode")
            val strategy = rulesInStep.first().strategy.uppercase()

            when (strategy) {
                "PARALLEL"  -> executeParallel(rulesInStep, packet, context)
                "CONSENSUS" -> executeConsensus(rulesInStep, packet, context)
                "ASYNC"     -> executeAsync(rulesInStep, packet, context)
                "RETRY"     -> executeWithRetry(rulesInStep, packet, context)
                else        -> executeSerial(rulesInStep, packet, context)
            }
        }
        println(EngineAnsi.CYAN + "🏁 [Engine-Core] Job ${context.getEventID()} Workflow Finished." + EngineAnsi.RESET)
    }

    // --- STRATEGIES ---

    private fun executeSerial(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        rules.forEach { rule ->
            if (shouldExecute(rule, packet)) runCartridge(rule, packet, context)
        }
    }

    // ⚡ SCATTER-GATHER (Parallel)
    private fun executeParallel(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val activeRules = rules.filter { shouldExecute(it, packet) }
        if (activeRules.isEmpty()) return

        println("      " + EngineAnsi.CYAN + "⚡ [Parallel] Forking ${activeRules.size} tasks..." + EngineAnsi.RESET)

        runBlocking {
            // SCATTER: Launch all tasks
            val jobs = activeRules.map { rule ->
                async(Dispatchers.Default) {
                    runCartridge(rule, packet, context)
                }
            }
            // GATHER: Wait for all to finish
            jobs.awaitAll()
        }
    }

    private fun executeConsensus(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        executeSerial(rules, packet, context)
    }

    private fun executeAsync(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val activeRules = rules.filter { shouldExecute(it, packet) }
        if (activeRules.isEmpty()) return

        // Fire and Forget
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

    // 🟢 RETRY LOGIC
    private fun executeWithRetry(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        rules.filter { shouldExecute(it, packet) }.forEach { rule ->
            val maxRetries = 3
            var attempt = 0
            var success = false
            var lastError: Exception? = null

            while (attempt < maxRetries && !success) {
                attempt++
                try {
                    if (attempt > 1) {
                        println(EngineAnsi.YELLOW + "      🔄 [Retry] Attempt $attempt/$maxRetries for ${rule.cartridgeId}..." + EngineAnsi.RESET)
                    }
                    runCartridge(rule, packet, context)
                    success = true
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < maxRetries) {
                        try { Thread.sleep(500) } catch (_: InterruptedException) {} // Backoff
                    }
                }
            }

            if (!success) {
                println(EngineAnsi.RED + "      ❌ [Retry] Giving up after $maxRetries attempts." + EngineAnsi.RESET)
                throw lastError ?: RuntimeException("Retry Failed")
            }
        }
    }

    // --- 🟢 LOGIC ENGINE ---

    private fun shouldExecute(rule: MatrixRule, packet: ExchangePacket): Boolean {
        val logic = rule.selectorLogic
        if (logic.isBlank() || logic == "*") return true

        try {
            if (logic.contains("==")) {
                val parts = logic.split("==")
                val key = parts[0].trim()
                val expectedValue = parts[1].trim().replace("'", "")
                val actualValue = packet.data[key]?.toString() ?: ""
                return actualValue == expectedValue
            }
        } catch (e: Exception) {
            return false
        }
        return true
    }

    private fun runCartridge(rule: MatrixRule, packet: ExchangePacket, context: KernelContext) {
        val cartridge = cartridgeMap[rule.cartridgeId]
        val stepCode = "[${rule.moduleId}-${rule.slotId}]"

        if (cartridge != null) {
            try {
                context.set("STEP_PREFIX", stepCode)

                // 🟢 CONFIG PARSING FIX
                if (rule.configJson.isNotBlank() && rule.configJson != "{}") {
                    try {
                        val configMap = objectMapper.readValue(rule.configJson, Map::class.java)
                        configMap.forEach { (key, value) ->
                            if (key != null && value != null) {
                                context.set(key.toString(), value)
                            }
                        }
                    } catch (e: Exception) {
                        println("      ⚠️ Config parsing failed for $stepCode")
                    }
                }

                cartridge.execute(packet, context)
                // 🟢 Log to DB (Async)
                logToDb(packet.id, rule, stepCode, "SUCCESS", "Executed")
            } catch (e: Exception) {
                // Only print error if NOT retrying (Caller handles retry logging)
                if (rule.strategy != "RETRY") {
                    println(EngineAnsi.RED + "      💥 Error in ${rule.cartridgeId}: ${e.message}" + EngineAnsi.RESET)
                }
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
            } catch (_: Exception) { }
        }
    }
}