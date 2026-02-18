package com.tsd.platform.engine.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.tsd.adapter.output.persistence.AuditRepository
import com.tsd.adapter.output.persistence.WorkflowRepository
import com.tsd.core.model.AuditLog
import com.tsd.core.model.WorkflowStatus // 🟢 Import Enum
import com.tsd.platform.config.loader.WorkflowLoader
import com.tsd.platform.engine.model.MatrixRule
import com.tsd.platform.engine.state.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.WorkflowEngine
import kotlinx.coroutines.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.Stack

@Service
@Primary
class EnterpriseWorkflowEngine(
    private val workflowLoader: WorkflowLoader,
    private val existingCartridges: List<Cartridge>,
    private val auditRepo: AuditRepository,
    private val workflowRepo: WorkflowRepository,
    private val objectMapper: ObjectMapper
    // 🟢 CLEAN ARCHITECTURE: No JdbcTemplate here!
) : WorkflowEngine {

    private val cartridgeMap: Map<String, Cartridge> = existingCartridges
        .groupBy { it.id }
        .mapValues { (id, versions) ->
            if (versions.size > 1) {
                val winner = versions.maxByOrNull { it.version }!!
                println(EngineAnsi.YELLOW + "   ⚠️ [Engine] Duplicate Cartridge ID '$id'. Using v${winner.version}." + EngineAnsi.RESET)
                winner
            } else {
                versions.first()
            }
        }

    // Manual Trigger Wrapper
    fun executeWorkflow(registrar: String, workflowId: String) {
        val jobId = "JOB-${System.currentTimeMillis()}"
        println(EngineAnsi.CYAN + "🚀 [Engine-Entry] Manual Trigger: $workflowId ($registrar) -> Job: $jobId" + EngineAnsi.RESET)

        val contextData = mapOf(
            "Registrar_Code" to registrar,
            "Workflow_ID" to workflowId,
            "AMOUNT" to 50000.00,
            "CURRENCY" to "THB",
            "INVESTOR_ID" to "ACCT-001",
            "WALLET_ID" to "W-998877",
            "TARGET_BANK" to "BBL_TH"
        )

        executeJob(jobId, contextData)
    }

    override fun executeJob(jobId: String, data: Map<String, Any>) {
        println(EngineAnsi.CYAN + "🚀 [Engine-Core] Starting Job: $jobId" + EngineAnsi.RESET)

        val workflowId = data["Workflow_ID"]?.toString() ?: "UNKNOWN"
        val jsonPayload = try {
            objectMapper.writeValueAsString(data)
        } catch (e: Exception) { "{}" }

        // 🟢 STEP 1: PERSIST INITIAL STATE
        try {
            // Refactor: Use Enum .name
            workflowRepo.save(jobId, workflowId, "INIT", WorkflowStatus.RUNNING.name, jsonPayload)
            println("   💾 [Persistence] Job Created in DB: $jobId (RUNNING)")
        } catch (e: Exception) {
            println("   ⚠️ [Persistence] Failed to save initial job state: ${e.message}")
        }

        println("   🔍 [Engine-Debug] Raw Input Keys: ${data.keys}")

        val packet = ExchangePacket(id = jobId, traceId = jobId)

        // 🟢 STEP 2: SANITIZE & LOAD DATA
        data.forEach { (k, v) ->
            val safeValue = when (v) {
                is Double -> java.math.BigDecimal.valueOf(v)
                is Float -> java.math.BigDecimal.valueOf(v.toDouble())
                is Int -> java.math.BigDecimal.valueOf(v.toLong())
                is Long -> java.math.BigDecimal.valueOf(v)
                else -> v
            }
            packet.data[k] = safeValue
            packet.data[k.lowercase()] = safeValue
        }

        if (packet.data.containsKey("AMOUNT")) packet.data["Net_Amount"] = packet.data["AMOUNT"]!!
        if (packet.data.containsKey("WALLET_ID")) packet.data["Account_ID"] = packet.data["WALLET_ID"]!!

        val context = KernelContext(jobId, "DEFAULT")
        packet.data.forEach { (k, v) -> context.set(k, v) }

        // 🟢 SAGA MEMORY
        val sagaStack = Stack<MatrixRule>()

        try {
            // 🟢 RUN WORKFLOW
            runWorkflow(packet, context, sagaStack)

            // 🟢 STEP 3: PERSIST SUCCESS STATE
            workflowRepo.save(jobId, workflowId, "END", WorkflowStatus.SETTLED.name, jsonPayload)
            println(EngineAnsi.CYAN + "🏁 [Engine-Core] Job $jobId Completed Successfully. State: SETTLED" + EngineAnsi.RESET)

        } catch (e: Exception) {
            println(EngineAnsi.RED + "💥 Job Failed: ${e.message}. Initiating SAGA COMPENSATION..." + EngineAnsi.RESET)

            // Perform Rollback
            performCompensation(sagaStack, packet, context)

            // 🟢 STEP 4: PERSIST FAILURE STATE
            workflowRepo.save(jobId, workflowId, "ERROR", WorkflowStatus.FAILED.name, jsonPayload)
            println(EngineAnsi.RED + "☠️ [Engine-Core] Job $jobId Terminated (Compensated). State: FAILED" + EngineAnsi.RESET)
            throw e
        }
    }

    private fun runWorkflow(packet: ExchangePacket, context: KernelContext, sagaStack: Stack<MatrixRule>) {
        val allRules = workflowLoader.rules
        val workflowId = context.getObject<String>("Workflow_ID") ?: "UNKNOWN"
        val registrarCode = context.getObject<String>("Registrar_Code") ?: "TSD"

        val activeRules = allRules.filter {
            it.registrarCode == registrarCode && it.workflowId == workflowId
        }

        if (activeRules.isEmpty()) {
            println(EngineAnsi.RED + "   ⚠️ No rules found for $registrarCode / $workflowId" + EngineAnsi.RESET)
            return
        }

        val stepGroups = activeRules.groupBy { "${it.moduleId}-${it.slotId}-${it.stepId}" }

        stepGroups.forEach { (_, rulesInStep) ->
            val strategy = rulesInStep.first().strategy.uppercase()

            when (strategy) {
                "PARALLEL"  -> executeParallel(rulesInStep, packet, context)
                "CONSENSUS" -> executeConsensus(rulesInStep, packet, context, sagaStack)
                "ASYNC"     -> executeAsync(rulesInStep, packet, context)
                "RETRY"     -> executeWithRetry(rulesInStep, packet, context, sagaStack)
                else        -> executeSerial(rulesInStep, packet, context, sagaStack)
            }
        }
    }

    // --- STRATEGIES ---

    private fun executeSerial(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext, sagaStack: Stack<MatrixRule>) {
        rules.forEach { rule ->
            val stepCode = "[${rule.moduleId}-${rule.slotId}]"

            // 🟢 REFACTORED: Call Repository for check
            if (workflowRepo.isStepAlreadyDone(packet.id, stepCode)) {
                println(EngineAnsi.GREEN + "      ⏩ [Smart-Skip] Step $stepCode already CLEARED. Added to Saga Stack." + EngineAnsi.RESET)
                sagaStack.push(rule)
                return@forEach
            }

            if (shouldExecute(rule, packet)) {
                runCartridge(rule, packet, context)
                sagaStack.push(rule)
            }
        }
    }

    private fun executeParallel(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val activeRules = rules.filter { shouldExecute(it, packet) }
        if (activeRules.isEmpty()) return

        println("      " + EngineAnsi.CYAN + "⚡ [Parallel] Forking ${activeRules.size} tasks..." + EngineAnsi.RESET)

        runBlocking {
            val jobs = activeRules.map { rule ->
                async(Dispatchers.Default) { runCartridge(rule, packet, context) }
            }
            jobs.awaitAll()
        }
    }

    private fun executeConsensus(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext, sagaStack: Stack<MatrixRule>) {
        executeSerial(rules, packet, context, sagaStack)
    }

    private fun executeAsync(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val activeRules = rules.filter { shouldExecute(it, packet) }
        if (activeRules.isEmpty()) return

        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch(Dispatchers.IO) {
            activeRules.forEach { rule ->
                try { runCartridge(rule, packet, context) }
                catch (e: Exception) { println(EngineAnsi.RED + "   ⚠️ Async Task Failed: ${e.message}" + EngineAnsi.RESET) }
            }
        }
    }

    private fun executeWithRetry(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext, sagaStack: Stack<MatrixRule>) {
        rules.filter { shouldExecute(it, packet) }.forEach { rule ->
            val maxRetries = 3
            var attempt = 0
            var success = false
            var lastError: Exception? = null

            while (attempt < maxRetries && !success) {
                attempt++
                try {
                    if (attempt > 1) println(EngineAnsi.YELLOW + "      🔄 [Retry] Attempt $attempt/$maxRetries..." + EngineAnsi.RESET)
                    runCartridge(rule, packet, context)
                    success = true
                    sagaStack.push(rule)
                } catch (e: Exception) {
                    lastError = e
                    if (attempt < maxRetries) try { Thread.sleep(500) } catch (_: InterruptedException) {}
                }
            }
            if (!success) throw lastError ?: RuntimeException("Retry Failed")
        }
    }

    // --- SAGA COMPENSATION LOGIC ---
    private fun performCompensation(stack: Stack<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        if (stack.isEmpty()) return

        println(EngineAnsi.YELLOW + "      ↩️ [Saga] Starting Compensation Rollback..." + EngineAnsi.RESET)

        while (stack.isNotEmpty()) {
            val rule = stack.pop()

            if (rule.isCompensatable) {
                val stepCode = "[${rule.moduleId}-${rule.slotId}]"
                try {
                    val cartridge = cartridgeMap[rule.cartridgeId]
                    if (cartridge != null) {
                        println("      🔙 [Undo] Reverting $stepCode via ${rule.cartridgeId}...")
                        cartridge.compensate(packet, context)
                        logToDb(packet.id, rule, stepCode, WorkflowStatus.FAILED, "COMPENSATED")
                    }
                } catch (e: Exception) {
                    println(EngineAnsi.RED + "      ❌ [Saga-Fail] Failed to compensate $stepCode: ${e.message}" + EngineAnsi.RESET)
                    logToDb(packet.id, rule, stepCode, WorkflowStatus.FAILED, "COMPENSATION_FAILED")
                }
            }
        }
        println(EngineAnsi.YELLOW + "      ✅ [Saga] Rollback Complete." + EngineAnsi.RESET)
    }

    // --- LOGIC & EXECUTION ---

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
        } catch (e: Exception) { return false }
        return true
    }

    private fun runCartridge(rule: MatrixRule, packet: ExchangePacket, context: KernelContext) {
        val cartridge = cartridgeMap[rule.cartridgeId]
        val stepCode = "[${rule.moduleId}-${rule.slotId}]"

        if (cartridge != null) {
            try {
                context.set("STEP_PREFIX", stepCode)
                if (rule.configJson.isNotBlank() && rule.configJson != "{}") {
                    try {
                        val configMap = objectMapper.readValue(rule.configJson, Map::class.java)
                        configMap.forEach { (k, v) -> if (k!=null && v!=null) context.set(k.toString(), v) }
                    } catch (_: Exception) {}
                }

                cartridge.execute(packet, context)
                logToDb(packet.id, rule, stepCode, WorkflowStatus.CLEARED, "Executed")

            } catch (e: Exception) {
                if (rule.strategy != "RETRY") println(EngineAnsi.RED + "      💥 Error in ${rule.cartridgeId}: ${e.message}" + EngineAnsi.RESET)
                logToDb(packet.id, rule, stepCode, WorkflowStatus.FAILED, e.message ?: "Error")
                throw e
            }
        } else {
            println(EngineAnsi.RED + "      ⚠️ Cartridge Not Found: ${rule.cartridgeId}" + EngineAnsi.RESET)
        }
    }

    private fun logToDb(traceId: String, rule: MatrixRule, stepCode: String, status: WorkflowStatus, msg: String) {
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