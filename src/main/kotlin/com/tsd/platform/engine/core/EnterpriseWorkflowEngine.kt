package com.tsd.platform.engine.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.tsd.adapter.out.persistence.AuditRepository
import com.tsd.core.model.AuditLog
import com.tsd.core.model.WorkflowStatus
import com.tsd.platform.config.loader.WorkflowLoader
import com.tsd.platform.engine.model.MatrixRule
import com.tsd.platform.engine.state.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.WorkflowEngine
import kotlinx.coroutines.*
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate // 🟢 NEW IMPORT
import org.springframework.stereotype.Service

@Service
@Primary
class EnterpriseWorkflowEngine(
    private val workflowLoader: WorkflowLoader,
    private val existingCartridges: List<Cartridge>,
    private val auditRepo: AuditRepository,
    private val objectMapper: ObjectMapper,
    private val jdbcTemplate: JdbcTemplate // 🟢 INJECT DB ACCESS
) : WorkflowEngine {

    // 🟢 INTELLIGENT LOADING: Filters duplicates and keeps only the highest version
    private val cartridgeMap: Map<String, Cartridge> = existingCartridges
        .groupBy { it.id }
        .mapValues { (id, versions) ->
            if (versions.size > 1) {
                val winner = versions.maxByOrNull { it.version }!!
                println(EngineAnsi.YELLOW + "   ⚠️ [Engine] Duplicate Cartridge ID '$id' detected. Using v${winner.version} (Ignored older versions)." + EngineAnsi.RESET)
                winner
            } else {
                versions.first()
            }
        }

    fun executeWorkflow(registrar: String, workflowId: String) {
        val jobId = "JOB-${System.currentTimeMillis()}"
        println(EngineAnsi.CYAN + "🚀 [Engine-Entry] Manual Trigger: $workflowId ($registrar) -> Job: $jobId" + EngineAnsi.RESET)

        // 🟢 UPDATED: Injecting REAL Mock Data so Cartridges don't crash or print "null"
        val contextData = mapOf(
            "Registrar_Code" to registrar,
            "Workflow_ID" to workflowId,
            "AMOUNT" to 50000.00,                 // 💰 50k THB
            "CURRENCY" to "THB",
            "INVESTOR_ID" to "ACCT-001",          // 👤 Matches your DataLoader mock
            "WALLET_ID" to "W-998877",
            "TARGET_BANK" to "BBL_TH"
        )

        executeJob(jobId, contextData)
    }

    override fun executeJob(jobId: String, data: Map<String, Any>) {
        println(EngineAnsi.CYAN + "🚀 [Engine-Core] Starting Job: $jobId" + EngineAnsi.RESET)

        // 🟢 STEP 1: PERSIST PAYLOAD (The "Black Box" Recorder)
        // We save the raw JSON immediately. If we crash 1ms later, we have the data to RESUME.
        try {
            val jsonPayload = objectMapper.writeValueAsString(data)
            jdbcTemplate.update(
                "INSERT INTO Workflow_Journal (Job_ID, Payload, Status, Created_At) VALUES (?, ?, ?, GETDATE())",
                jobId, jsonPayload, "INIT"
            )
            println("   💾 [Journal] Payload saved to Database.")
        } catch (e: Exception) {
            println("   ⚠️ [Journal] Failed to persist payload: ${e.message}")
            // We continue anyway, but risk data loss if crash occurs
        }

        // 🟢 DEBUG: Print raw input keys
        println("   🔍 [Engine-Debug] Raw Input Keys: ${data.keys}")

        // 🟢 STATE MACHINE: Start at INIT
        var currentStatus = WorkflowStatus.INIT

        val packet = ExchangePacket(id = jobId, traceId = jobId)

        // 🟢 STEP 2: SANITIZE & LOAD DATA
        data.forEach { (k, v) ->
            // Convert numbers to BigDecimal (Financial Safety)
            val safeValue = when (v) {
                is Double -> java.math.BigDecimal.valueOf(v)
                is Float -> java.math.BigDecimal.valueOf(v.toDouble())
                is Int -> java.math.BigDecimal.valueOf(v.toLong())
                is Long -> java.math.BigDecimal.valueOf(v)
                else -> v
            }
            // Store Original Key
            packet.data[k] = safeValue
            // Store Lowercase Key (Helper)
            packet.data[k.lowercase()] = safeValue
        }

        // 🟢 STEP 3: TRANSLATION LAYER
        // Map "AMOUNT" -> "Net_Amount"
        if (packet.data.containsKey("AMOUNT")) {
            packet.data["Net_Amount"] = packet.data["AMOUNT"]!!
        }
        // Map "WALLET_ID" -> "Account_ID"
        if (packet.data.containsKey("WALLET_ID")) {
            packet.data["Account_ID"] = packet.data["WALLET_ID"]!!
        }

        val context = KernelContext(jobId, "DEFAULT")
        packet.data.forEach { (k, v) -> context.set(k, v) }

        try {
            // TRANSITION: INIT -> PENDING
            currentStatus.verifyTransition(WorkflowStatus.PENDING)
            currentStatus = WorkflowStatus.PENDING
            updateJournalStatus(jobId, "PENDING") // 🟢 Update Journal

            runWorkflow(packet, context)

            // TRANSITION: PENDING -> CLEARED -> SETTLED
            currentStatus.verifyTransition(WorkflowStatus.CLEARED)
            currentStatus = WorkflowStatus.CLEARED

            currentStatus.verifyTransition(WorkflowStatus.SETTLED)
            currentStatus = WorkflowStatus.SETTLED
            updateJournalStatus(jobId, "SETTLED") // 🟢 Update Journal

            println(EngineAnsi.CYAN + "🏁 [Engine-Core] Job $jobId Completed Successfully. State: $currentStatus" + EngineAnsi.RESET)

        } catch (e: Exception) {
            currentStatus = WorkflowStatus.FAILED
            updateJournalStatus(jobId, "FAILED") // 🟢 Update Journal
            println(EngineAnsi.RED + "💥 Job Failed: ${e.message}. State: $currentStatus" + EngineAnsi.RESET)
            throw e
        }
    }

    // 🟢 HELPER: Updates the Journal Table
    private fun updateJournalStatus(jobId: String, status: String) {
        try {
            jdbcTemplate.update("UPDATE Workflow_Journal SET Status = ? WHERE Job_ID = ?", status, jobId)
        } catch (e: Exception) { /* Ignore non-critical DB errors */ }
    }

    private fun runWorkflow(packet: ExchangePacket, context: KernelContext) {
        val allRules = workflowLoader.rules
        val workflowId = context.getObject<String>("Workflow_ID") ?: "UNKNOWN"
        val registrarCode = context.getObject<String>("Registrar_Code") ?: "TSD"

        val activeRules = allRules.filter {
            it.registrarCode == registrarCode && it.workflowId == workflowId
        }

        if (activeRules.isEmpty()) {
            println(EngineAnsi.RED + "   ⚠️ No rules found for Registrar: $registrarCode, Workflow_ID: $workflowId" + EngineAnsi.RESET)
            return
        }

        val stepGroups = activeRules.groupBy { "${it.moduleId}-${it.slotId}-${it.stepId}" }

        stepGroups.forEach { (stepCode, rulesInStep) ->
            val strategy = rulesInStep.first().strategy.uppercase()

            when (strategy) {
                "PARALLEL"  -> executeParallel(rulesInStep, packet, context)
                "CONSENSUS" -> executeConsensus(rulesInStep, packet, context)
                "ASYNC"     -> executeAsync(rulesInStep, packet, context)
                "RETRY"     -> executeWithRetry(rulesInStep, packet, context)
                else        -> executeSerial(rulesInStep, packet, context)
            }
        }
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
                        try { Thread.sleep(500) } catch (_: InterruptedException) {}
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

                logToDb(packet.id, rule, stepCode, WorkflowStatus.CLEARED, "Executed")

            } catch (e: Exception) {
                if (rule.strategy != "RETRY") {
                    println(EngineAnsi.RED + "      💥 Error in ${rule.cartridgeId}: ${e.message}" + EngineAnsi.RESET)
                }

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