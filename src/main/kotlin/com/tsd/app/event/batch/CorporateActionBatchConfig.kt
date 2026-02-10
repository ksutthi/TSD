package com.tsd.app.event.batch

import com.tsd.app.account.model.AccountBalance
import com.tsd.app.account.repo.AccountBalanceRepository
import com.tsd.platform.config.ConfigMatrix
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.exception.JobSuspensionException
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.model.registry.ExecutionBlock
import com.tsd.platform.model.registry.MatrixRule
import com.tsd.platform.persistence.SuspendedAction
import com.tsd.platform.persistence.SuspendedActionRepository
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.data.RepositoryItemReader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager
import java.text.DecimalFormat
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Configuration
@EnableBatchProcessing
class CorporateActionBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val accountRepo: AccountBalanceRepository,
    private val configMatrix: ConfigMatrix,
    private val suspensionRepo: SuspendedActionRepository,
    cartridges: List<Cartridge>
) {

    private val logger = LoggerFactory.getLogger(CorporateActionBatchConfig::class.java)
    private val moneyFmt = DecimalFormat("#,##0.00")

    // 🟢 NEW: Global Shared State (The "Brain" that survives across steps)
    private val globalJobState = ConcurrentHashMap<String, Any>()

    private val cartridgeMap = cartridges.associateBy { it.id }.toMutableMap().apply {
        this["Flag_High_Value_Auth"] = this["Call_Identity_Mgmt"] ?: this["Flag_High_Value_Auth"] ?: return@apply
        this["Apply_WHT_Standard"] = this["Tax_Engine"] ?: return@apply
        this["Calc_Gross_Benefit"] = this["Calc_Gross_Benefit"] ?: this["Tax_Engine"] ?: return@apply
    }

    private val stubCartridge = cartridges.find { it.id == "Functional_Debug_Stub" }

    @Bean
    fun corporateActionJob(): Job {
        val builder = JobBuilder("CorporateActionJob", jobRepository)
        val plan = configMatrix.getExecutionPlan()

        var jobFlow = builder
            .listener(JobCompletionNotificationListener())
            .start(startStep())

        plan.forEach { block ->
            val stepName = "Step_${block.uniqueId}"
            jobFlow.next(createChunkStep(stepName, block))
        }

        return jobFlow.build()
    }

    @Bean
    fun startStep(): Step {
        return StepBuilder("Step_Start", jobRepository)
            .tasklet({ _, _ ->
                println("\n============================================================")
                println(" STARTING BATCH JOB: Corporate Action Distribution")
                println("============================================================")
                // Clear state at start of new job
                globalJobState.clear()
                null
            }, transactionManager)
            .build()
    }

    private fun createChunkStep(stepName: String, block: ExecutionBlock): Step {
        return StepBuilder(stepName, jobRepository)
            .chunk<AccountBalance, AccountBalance>(10, transactionManager)
            .reader(accountReader())
            .writer(blockWriter(block))
            .listener(BlockListener(block))
            .faultTolerant()
            .skip(Exception::class.java)
            .noSkip(JobSuspensionException::class.java)
            .skipLimit(100)
            .build()
    }

    @Bean
    fun accountReader(): RepositoryItemReader<AccountBalance> {
        val reader = RepositoryItemReader<AccountBalance>()
        reader.setRepository(accountRepo)
        reader.setMethodName("findAll")
        reader.setSort(Collections.singletonMap("accountId", Sort.Direction.ASC))
        return reader
    }

    private fun blockWriter(block: ExecutionBlock): ItemWriter<AccountBalance> {
        return ItemWriter { accounts ->
            if (block.scope.equals("JOB", ignoreCase = true)) {
                executeBlockLogic(null, block)
            } else {
                accounts.forEach { account ->
                    executeBlockLogic(account, block)
                }
            }
        }
    }

    // ==================================================================================
    // 🚀 THE "BIG 5" EXECUTION ENGINE (Fixed Data Persistence)
    // ==================================================================================
    private fun executeBlockLogic(account: AccountBalance?, block: ExecutionBlock) {
        val contextData = mutableMapOf<String, Any>()

        // 1. 🟢 LOAD GLOBAL STATE (Inject "Rate" from previous steps)
        contextData.putAll(globalJobState)

        // 2. Setup Account Context
        if (account != null) {
            val freshAccount = accountRepo.findById(account.accountId).orElse(account)
            contextData["Account_ID"] = freshAccount.accountId
            contextData["Tax_Profile"] = freshAccount.taxProfile
            contextData["Country_Code"] = freshAccount.countryCode
            contextData["Balance"] = freshAccount.shareBalance
            contextData["Share_Balance"] = freshAccount.shareBalance
        }

        // 3. Prepare Context
        val context = KernelContext(jobId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8))
        contextData.forEach { (k, v) -> context.set(k, v) }

        val packet = ExchangePacket(
            id = account?.accountId?.toString() ?: "JOB",
            data = contextData
        )

        // Header Printing
        if (account != null) {
            println(EngineAnsi.CYAN + " [Account ${account.accountId}]" + EngineAnsi.RESET)
        } else {
            println(EngineAnsi.CYAN + " [JOB LEVEL]" + EngineAnsi.RESET)
        }

        // 4. EXECUTE RULES
        val steps = block.rules.groupBy { it.stepId }

        steps.forEach { (_, rulesInStep) ->
            val strategy = rulesInStep.first().strategy.uppercase()
            try {
                when (strategy) {
                    "SERIAL"      -> executeSerial(rulesInStep, packet, context)
                    "PARALLEL"    -> executeParallel(rulesInStep, packet, context)
                    "CONSENSUS"   -> executeConsensus(rulesInStep, packet, context)
                    "FIRST_MATCH" -> executeFirstMatch(rulesInStep, packet, context)
                    "REMOTE"      -> executeRemote(rulesInStep, packet, context)
                    else          -> executeSerial(rulesInStep, packet, context)
                }
            } catch (e: Exception) {
                println(EngineAnsi.RED + "    CRITICAL FAILURE in [$strategy]: ${e.message}" + EngineAnsi.RESET)
                throw e
            }
        }

// 5. 🟢 SAVE GLOBAL STATE (The "X-Ray" Fix)
        if (block.scope.equals("JOB", ignoreCase = true)) {
            try {
                println(EngineAnsi.MAGENTA + "    🕵️ [X-RAY] Peeking inside KernelContext..." + EngineAnsi.RESET)

                // 1. REFLECTION: Unlock the private 'memory' map
                val field = context.javaClass.getDeclaredField("memory")
                field.isAccessible = true

                @Suppress("UNCHECKED_CAST")
                val secretMap = field.get(context) as Map<String, Any>

                // 2. LOGGING: Print exactly what we found
                println(EngineAnsi.MAGENTA + "    🔑 KEYS FOUND: " + secretMap.keys + EngineAnsi.RESET)

                // 3. PERSISTENCE: Save EVERYTHING found (No guessing!)
                secretMap.forEach { (key, value) ->
                    globalJobState[key] = value

                    // 4. INTELLIGENT ALIASING
                    // If the key looks like a rate (e.g. "Dividend_Rate"),
                    // ALSO save it as "Rate" so the calculator finds it.
                    if (key.contains("Rate", ignoreCase = true) || key.contains("Dividend", ignoreCase = true)) {
                        globalJobState["Rate"] = value
                        println(EngineAnsi.YELLOW + "    💾 ALIAS CREATED: Rate = $value (from $key)" + EngineAnsi.RESET)
                    } else {
                        println(EngineAnsi.YELLOW + "    💾 PERSISTED GLOBAL: $key = $value" + EngineAnsi.RESET)
                    }
                }
            } catch (e: Exception) {
                println(EngineAnsi.RED + "    X-RAY FAILED: ${e.message}" + EngineAnsi.RESET)
            }
        }
    }

    // --- STRATEGIES (Unchanged) ---
    private fun executeSerial(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        for (rule in rules) executeRule(rule, packet, context, "")
    }

    private fun executeParallel(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        if (rules.isEmpty()) return
        println(EngineAnsi.MAGENTA + "    [PARALLEL] Spawning ${rules.size} threads..." + EngineAnsi.RESET)
        rules.parallelStream().forEach { rule -> executeRule(rule, packet, context, "[PARALLEL]") }
    }

    private fun executeConsensus(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        if (rules.isEmpty()) return
        println(EngineAnsi.YELLOW + "    [CONSENSUS] Requesting ${rules.size} votes..." + EngineAnsi.RESET)
        rules.forEach { rule -> executeRule(rule, packet, context, "[CONSENSUS]") }
    }

    private fun executeFirstMatch(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        if (rules.isEmpty()) return
        println(EngineAnsi.GREEN + "    [FIRST_MATCH] Hunting for a winner..." + EngineAnsi.RESET)
        var winnerFound = false
        for ((index, rule) in rules.withIndex()) {
            try {
                executeRule(rule, packet, context, "[FIRST_MATCH]")
                println(EngineAnsi.GREEN + "        WINNER FOUND! Skipping remaining ${rules.size - (index + 1)} rules." + EngineAnsi.RESET)
                winnerFound = true
                break
            } catch (e: Exception) {
                println(EngineAnsi.YELLOW + "        Rule failed (${e.message}). Trying next candidate..." + EngineAnsi.RESET)
            }
        }
        if (!winnerFound) throw RuntimeException("All ${rules.size} candidates in FIRST_MATCH failed.")
    }

    private fun executeRemote(rules: List<MatrixRule>, packet: ExchangePacket, context: KernelContext) {
        if (rules.isEmpty()) return
        println(EngineAnsi.BLUE + "    [REMOTE] Initializing Uplink..." + EngineAnsi.RESET)
        rules.forEach { rule ->
            val startTime = System.currentTimeMillis()
            executeRule(rule, packet, context, "[REMOTE]")
            val duration = System.currentTimeMillis() - startTime
            println(EngineAnsi.BLUE + "        Latency: ${duration}ms" + EngineAnsi.RESET)
        }
    }

    private fun executeRule(rule: MatrixRule, packet: ExchangePacket, context: KernelContext, tag: String) {
        var cartridge = cartridgeMap[rule.cartridgeId]
        if (cartridge == null) cartridge = cartridgeMap[rule.cartridgeName]
        if (cartridge == null) cartridge = stubCartridge

        if (cartridge != null) {
            val stepCode = "[${rule.slotId}-S${rule.stepId}]"
            context.set("STEP_PREFIX", stepCode)

            synchronized(System.out) {
                val safeTag = if (tag.isNotEmpty()) tag.padEnd(12) else "            "
                val structInfo = EngineAnsi.BLUE + stepCode.padEnd(8) + EngineAnsi.RESET
                val nameInfo = EngineAnsi.YELLOW + rule.cartridgeName + EngineAnsi.RESET
                println("    $structInfo $safeTag $nameInfo ... " + EngineAnsi.CYAN + "[STARTED]" + EngineAnsi.RESET)
            }

            try {
                cartridge.execute(packet, context)
                synchronized(System.out) {
                    val safeTag = if (tag.isNotEmpty()) tag.padEnd(12) else "            "
                    val structInfo = EngineAnsi.BLUE + stepCode.padEnd(8) + EngineAnsi.RESET
                    println("    $structInfo $safeTag " + EngineAnsi.GREEN + "[COMPLETED]" + EngineAnsi.RESET)
                }
                context.set("SUCCESS_FLAG", true)
            } catch (e: Exception) {
                synchronized(System.out) {
                    val safeTag = if (tag.isNotEmpty()) tag.padEnd(12) else "            "
                    val structInfo = EngineAnsi.BLUE + stepCode.padEnd(8) + EngineAnsi.RESET
                    println("    $structInfo $safeTag " + EngineAnsi.RED + "[FAILED]: ${e.message}" + EngineAnsi.RESET)
                }
                throw e
            }
        } else {
            synchronized(System.out) {
                println(EngineAnsi.RED + "MISSING CARTRIDGE: ${rule.cartridgeName}" + EngineAnsi.RESET)
            }
            throw RuntimeException("Cartridge ${rule.cartridgeName} not found")
        }
    }

    class JobCompletionNotificationListener : JobExecutionListener {
        override fun afterJob(jobExecution: JobExecution) {
            if (jobExecution.status == BatchStatus.COMPLETED) {
                println("\n============================================================")
                println(" BATCH JOB COMPLETED SUCCESSFULLY")
                println("============================================================")
            } else {
                println(" BATCH JOB FAILED WITH STATUS: ${jobExecution.status}")
            }
        }
    }

    class BlockListener(private val block: ExecutionBlock) : org.springframework.batch.core.StepExecutionListener {
        override fun beforeStep(stepExecution: org.springframework.batch.core.StepExecution) {
            val moduleName = block.rules.firstOrNull()?.moduleName ?: "Unknown Module"
            println(EngineAnsi.RESET + "\n------------------------------------------------------------")
            println(EngineAnsi.BOLD_CYAN + " MODULE [${block.moduleId}] $moduleName " + EngineAnsi.RESET + EngineAnsi.WHITE + "| Rules: ${block.rules.size} | Scope: ${block.scope}" + EngineAnsi.RESET)
            println(EngineAnsi.BOLD_WHITE + "------------------------------------------------------------" + EngineAnsi.RESET)
        }
    }
}