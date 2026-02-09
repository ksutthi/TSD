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

        // 🟢 Added Listener to print "Job Finished"
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
                println("\n" + EngineAnsi.BOLD_WHITE + "============================================================" + EngineAnsi.RESET)
                println(EngineAnsi.BOLD_GREEN + "🚀 STARTING BATCH JOB: Corporate Action Distribution" + EngineAnsi.RESET)
                println(EngineAnsi.BOLD_WHITE + "============================================================" + EngineAnsi.RESET)
                null
            }, transactionManager)
            .build()
    }

    private fun createChunkStep(stepName: String, block: ExecutionBlock): Step {
        return StepBuilder(stepName, jobRepository)
            .chunk<AccountBalance, AccountBalance>(10, transactionManager)
            .reader(accountReader())
            .writer(blockWriter(block))
            .listener(BlockListener(block)) // Prints the Module Headers
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

    private fun executeBlockLogic(account: AccountBalance?, block: ExecutionBlock) {
        val contextData = mutableMapOf<String, Any>()

        // 1. Setup the Rate
        val rate = java.math.BigDecimal("4.00")
        contextData["Rate"] = rate
        contextData["rate"] = rate

        if (account != null) {
            // 🟢 FRESH ACCOUNT: Reload from DB to ensure we have the latest numbers
            val freshAccount = accountRepo.findById(account.accountId).orElse(account)

            contextData["Account_ID"] = freshAccount.accountId
            contextData["Tax_Profile"] = freshAccount.taxProfile
            contextData["Country_Code"] = freshAccount.countryCode

            val balance = freshAccount.shareBalance

            // 🟢 CRITICAL MAPPING:
            // We give it BOTH names so every cartridge is happy.
            contextData["Share_Balance"] = balance  // For Ingest
            contextData["Balance"] = balance        // For Sanity Check (Fixes "Zero Balance")

            // 🟢 LOGIC FIX: Net vs Gross
            val gross = balance.multiply(rate)
            val taxRate = java.math.BigDecimal("0.10") // 10% Tax
            val estimatedTax = gross.multiply(taxRate)
            val net = gross.subtract(estimatedTax)

            contextData["Gross_Amount"] = gross
            contextData["GROSS_AMOUNT"] = gross
            contextData["Net_Amount"] = net  // Now logically correct (Gross - Tax)
        }

        val context = KernelContext(jobId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8))
        contextData.forEach { (k, v) -> context.set(k, v) }

        val packet = ExchangePacket(
            id = account?.accountId?.toString() ?: "JOB",
            data = contextData
        )

        // Header Printing
        if (account != null) {
            println(EngineAnsi.CYAN + "👤 [Account ${account.accountId}]" + EngineAnsi.RESET)
        } else {
            println(EngineAnsi.CYAN + "⚙️ [JOB LEVEL]" + EngineAnsi.RESET)
        }

        // Rule Execution Loop
        for (rule in block.rules) {
            try {
                var cartridge = cartridgeMap[rule.cartridgeId]
                if (cartridge == null) cartridge = cartridgeMap[rule.cartridgeName]
                if (cartridge == null) cartridge = stubCartridge

                if (cartridge != null) {
                    // 🟢 UPDATE: Standardized numbering [Slot-Step] e.g. [K1-S1]
                    val stepCode = "[${rule.slotId}-S${rule.stepId}]"
                    // 👇 ADD THIS SINGLE LINE HERE 👇
                    context.set("STEP_PREFIX", stepCode)
                    // 👆 THIS PASSES THE ID TO THE CARTRIDGE 👆
                    val structInfo = EngineAnsi.BLUE + stepCode.padEnd(8) + EngineAnsi.RESET
                    val nameInfo = EngineAnsi.YELLOW + rule.cartridgeName + EngineAnsi.RESET
                    val indent = "    "

                    // 🟢 UPDATE: Detective Debugging to find the Zero Balance cause
                    if (rule.cartridgeName == "Sanity_Check_Positions") {
                        val b = contextData["Balance"]
                    }

                    print("$indent$structInfo $nameInfo >> ")

                    cartridge.execute(packet, context)
                } else {
                    println(EngineAnsi.RED + "⚠️ MISSING" + EngineAnsi.RESET)
                }

            } catch (e: JobSuspensionException) {
                println("")
                println("    " + EngineAnsi.RED + "    🛑 SUSPENDED: ${formatSuspensionMessage(e.message)}" + EngineAnsi.RESET)
                saveSuspension(account?.accountId ?: 0L, rule, e, contextData)
                return

            } catch (e: Exception) {
                println("    " + EngineAnsi.RED + "    ⚠️ SKIP: ${e.message}" + EngineAnsi.RESET)
                throw e
            }
        }
    }

    private fun formatSuspensionMessage(msg: String?): String {
        if (msg == null) return "Unknown Error"
        return try {
            val regex = "(\\d+\\.\\d+E\\d+|\\d+\\.\\d+)".toRegex()
            msg.replace(regex) { match ->
                try {
                    val num = match.value.toDouble()
                    moneyFmt.format(num)
                } catch (e: Exception) {
                    match.value
                }
            }
        } catch (e: Exception) {
            msg
        }
    }

    private fun saveSuspension(accountId: Long, rule: MatrixRule, e: JobSuspensionException, data: Map<String, Any>) {
        try {
            if (!suspensionRepo.existsByAccountIdAndCartridgeName(accountId, rule.cartridgeName)) {
                val action = SuspendedAction(
                    accountId = accountId,
                    cartridgeName = rule.cartridgeName,
                    suspenseCode = e.suspenseCode,
                    reason = e.reason,
                    status = "PENDING",
                    resolutionType = "RETRY",
                    contextData = data
                )
                suspensionRepo.save(action)
                println("      " + EngineAnsi.MAGENTA + "📥 TICKET CREATED [ID: $accountId]" + EngineAnsi.RESET)
            }
        } catch (ex: Exception) {
            println("      ⚠️ Failed to save suspension: ${ex.message}")
        }
    }

    class JobCompletionNotificationListener : JobExecutionListener {
        override fun afterJob(jobExecution: JobExecution) {
            if (jobExecution.status == BatchStatus.COMPLETED) {
                println("\n" + EngineAnsi.BOLD_WHITE + "============================================================" + EngineAnsi.RESET)
                println(EngineAnsi.BOLD_GREEN + "✅ BATCH JOB COMPLETED SUCCESSFULLY" + EngineAnsi.RESET)
                println(EngineAnsi.BOLD_WHITE + "============================================================" + EngineAnsi.RESET)
            } else {
                println(EngineAnsi.BOLD_RED + "❌ BATCH JOB FAILED WITH STATUS: ${jobExecution.status}" + EngineAnsi.RESET)
            }
        }
    }

    // 🟢 Replace just this class at the bottom of the file
    class BlockListener(private val block: ExecutionBlock) : org.springframework.batch.core.StepExecutionListener {
        override fun beforeStep(stepExecution: org.springframework.batch.core.StepExecution) {
            // 1. Extract the human-readable name from the first rule in this block
            val moduleName = block.rules.firstOrNull()?.moduleName ?: "Unknown Module"

            println(EngineAnsi.RESET + "\n" + EngineAnsi.BOLD_WHITE + "------------------------------------------------------------" + EngineAnsi.RESET)
            // 2. Print it here: [K] Eligibility Determination
            println(EngineAnsi.BOLD_CYAN + "📦 MODULE [${block.moduleId}] $moduleName " + EngineAnsi.RESET + EngineAnsi.WHITE + "| Rules: ${block.rules.size} | Scope: ${block.scope}" + EngineAnsi.RESET)
            println(EngineAnsi.BOLD_WHITE + "------------------------------------------------------------" + EngineAnsi.RESET)
        }
    }
}

