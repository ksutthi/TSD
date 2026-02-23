package com.tsd.platform.engine.batch

import com.tsd.platform.engine.core.EnterpriseWorkflowEngine
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.ListItemReader
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.SimpleAsyncTaskExecutor // 🟢 NEW IMPORT
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class PlatformBatchConfig(
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val engine: EnterpriseWorkflowEngine
) {

    @Bean
    fun universalEnterpriseBatchJob(universalProcessStep: Step): Job {
        return JobBuilder("universalEnterpriseBatchJob", jobRepository)
            .start(universalProcessStep)
            .build()
    }

    @Bean
    fun universalProcessStep(
        dynamicReader: ItemReader<Map<String, Any>>,
        dynamicProcessor: ItemProcessor<Map<String, Any>, ExchangePacket>,
        dynamicWriter: ItemWriter<ExchangePacket>
    ): Step {
        return StepBuilder("universalProcessStep", jobRepository)
            .chunk<Map<String, Any>, ExchangePacket>(10, transactionManager)
            .reader(dynamicReader)
            .processor(dynamicProcessor)
            .writer(dynamicWriter)
            .build()
    }

    // =========================================================================
    // 📦 GENERIC MVP COMPONENTS
    // =========================================================================

    @Bean
    @JobScope
    fun dynamicReader(
        @Value("#{jobParameters['record_count']}") recordCount: Long?
    ): ItemReader<Map<String, Any>> {
        val count = recordCount?.toInt() ?: 100

        val mockData = (1..count).map { i ->
            mapOf(
                "INVESTOR_ID" to "ACCT-${i.toString().padStart(4, '0')}",
                "WALLET_ID" to "W-99${i}",
                "AMOUNT" to 50000.00,
                "CURRENCY" to "THB",
                "TARGET_BANK" to "BBL_TH",
                "Payment_Mode" to "Standard"
            )
        }
        println(EngineAnsi.CYAN + "📦 [Batch-Reader] Loaded $count raw records into memory." + EngineAnsi.RESET)
        return ListItemReader(mockData)
    }

    @Bean
    @JobScope
    fun dynamicProcessor(
        @Value("#{jobParameters['registrar']}") registrar: String?,
        @Value("#{jobParameters['workflowId']}") workflowId: String?
    ): ItemProcessor<Map<String, Any>, ExchangePacket> {

        return ItemProcessor { rawData ->
            val jobId = "BATCH-${System.currentTimeMillis()}-${(1000..9999).random()}"

            val contextData = rawData.toMutableMap()
            contextData["Registrar_Code"] = registrar ?: "UNKNOWN"
            contextData["Workflow_ID"] = workflowId ?: "UNKNOWN"

            try {
                engine.executeJob(jobId, contextData)

                val packet = ExchangePacket(id = jobId, traceId = jobId)
                packet.data.putAll(contextData)
                packet
            } catch (e: Exception) {
                println(EngineAnsi.RED + "💥 [Batch-Processor] Row Failed: ${e.message}" + EngineAnsi.RESET)
                null
            }
        }
    }

    @Bean
    fun dynamicWriter(): ItemWriter<ExchangePacket> {
        return ItemWriter { chunk ->
            println(EngineAnsi.GREEN + "💾 [Batch-Writer] Bulk saving chunk of ${chunk.items.size} processed packets..." + EngineAnsi.RESET)
            chunk.items.forEach { packet ->
                println("      -> Processed: ${packet.id} for Investor: ${packet.data["INVESTOR_ID"]}")
            }
        }
    }

    // =========================================================================
    // 🚀 ASYNCHRONOUS LAUNCHER (Prevents API Timeouts)
    // =========================================================================
    @Bean("asyncJobLauncher")
    fun asyncJobLauncher(jobRepository: JobRepository): JobLauncher {
        val jobLauncher = TaskExecutorJobLauncher()
        jobLauncher.setJobRepository(jobRepository)
        // This makes it fire-and-forget!
        jobLauncher.setTaskExecutor(SimpleAsyncTaskExecutor())
        try {
            jobLauncher.afterPropertiesSet()
        } catch (e: Exception) {
            throw RuntimeException("Failed to configure Async Job Launcher", e)
        }
        return jobLauncher
    }
}