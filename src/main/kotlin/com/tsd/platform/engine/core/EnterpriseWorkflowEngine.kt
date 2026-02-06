package com.tsd.platform.engine.core

import com.tsd.platform.engine.loader.WorkflowLoader // ✅ Correct Loader
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.model.MatrixRule
import com.tsd.platform.persistence.AuditLog
import com.tsd.platform.persistence.AuditRepository
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.WorkflowEngine
import com.tsd.platform.spi.Cartridge // ✅ Needed for Cartridge type
import kotlinx.coroutines.*
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
@Primary
class EnterpriseWorkflowEngine(
    private val workflowLoader: WorkflowLoader,   // ✅ Use your existing Loader
    private val existingCartridges: List<Cartridge>, // ✅ Inject List of Cartridges
    private val consensusService: ConsensusService,
    private val auditRepo: AuditRepository
) : WorkflowEngine {

    // ⚡ Fast Lookup Map: "CartridgeID" -> Instance
    private val cartridgeMap = existingCartridges.associateBy { it.id }

    override fun executeJob(jobId: String, data: Map<String, Any>) {
        println(EngineAnsi.CYAN + "🚀 [Engine-Core] Starting Job: $jobId" + EngineAnsi.RESET)

        // 1. Inflate Interface Data -> Internal Packet
        val packet = ExchangePacket(
            id = jobId,
            traceId = jobId // ✅ Use traceId instead of tenant
        )
        // Populate packet data safely
        packet.data.putAll(data)

        // 2. Create Context
        val context = KernelContext(jobId, "DEFAULT")
        data.forEach { (k, v) -> context.set(k, v) } // ✅ Use .set()

        // 3. Run Strategies
        runWorkflow(packet, context)
    }

    private fun runWorkflow(packet: ExchangePacket, context: KernelContext) {
        val allRules = workflowLoader.rules // ✅ Use correct rules source

        // Group by Unique Step ID to handle Parallel/Serial blocks
        val stepGroups = allRules.groupBy { "${it.moduleId}-${it.slotId}-${it.stepId}" }

        stepGroups.forEach { (_, rulesInStep) -> // ✅ Use _ for unused key to fix ambiguity
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

    // 1. 🚶 SERIAL
    private fun executeSerial(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        rules.forEach { rule ->
            if (shouldExecute(rule, packet)) runCartridge(rule, packet, context)
        }
    }

    // 2. ⚡ PARALLEL
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

    // 3. ✋ CONSENSUS
    private fun executeConsensus(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        // Run logic normally; the Cartridge itself calls consensusService.waitForConsensus()
        executeSerial(rules, packet, context)
    }

    // 4. 📨 ASYNC
    private fun executeAsync(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val activeRules = rules.filter { shouldExecute(it, packet) }
        if (activeRules.isEmpty()) return

        println("      " + EngineAnsi.CYAN + "📨 [Async] Kicking off ${activeRules.size} tasks..." + EngineAnsi.RESET)

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

    // 5. 🔄 RETRY
    private fun executeWithRetry(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        val maxRetries = 3
        rules.filter { shouldExecute(it, packet) }.forEach { rule ->
            var attempt = 1
            var success = false
            while (attempt <= maxRetries && !success) {
                try {
                    runCartridge(rule, packet, context)
                    success = true
                } catch (e: Exception) {
                    println(EngineAnsi.RED + "      🔄 [Retry] Attempt $attempt failed. Retrying..." + EngineAnsi.RESET)
                    Thread.sleep(1000)
                    attempt++
                }
            }
            if (!success) throw RuntimeException("Step Failed after $maxRetries attempts")
        }
    }

    // --- HELPERS ---

    private fun shouldExecute(rule: MatrixRule, packet: ExchangePacket): Boolean {
        return rule.selector.isBlank() || rule.selector == "*"
    }

    private fun runCartridge(rule: MatrixRule, packet: ExchangePacket, context: KernelContext) {
        // ✅ Use Map Lookup instead of generic loader
        val cartridge = cartridgeMap[rule.cartridgeId]
        val stepCode = "[${rule.slotId}-S${rule.stepId}]"

        if (cartridge != null) {
            try {
                context.set("STEP_PREFIX", stepCode) // ✅ Use .set()
                cartridge.execute(packet, context)

                // 🟢 Log Success
                logToDb(packet.id, rule, stepCode, "SUCCESS", "Executed Successfully")

            } catch (e: Exception) {
                println(EngineAnsi.RED + "      💥 Error in ${rule.cartridgeId}: ${e.message}" + EngineAnsi.RESET)

                // 🟢 Log Failure
                logToDb(packet.id, rule, stepCode, "FAILED", e.message ?: "Unknown Error")

                throw e
            }
        } else {
            println(EngineAnsi.RED + "      ⚠️ Cartridge Not Found: ${rule.cartridgeId}" + EngineAnsi.RESET)

            // 🟢 Log Missing
            logToDb(packet.id, rule, stepCode, "ERROR", "Cartridge Bean Not Found")
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
                    cartridge = rule.cartridgeId, // ✅ Use cartridgeId, not name
                    status = status,
                    message = msg
                )
                auditRepo.save(log)
            } catch (e: Exception) {
                println("   ⚠️ Failed to save audit log: ${e.message}")
            }
        }
    }
}