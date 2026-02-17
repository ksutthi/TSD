package com.tsd

import com.tsd.platform.engine.core.EnterpriseWorkflowEngine
import kotlinx.coroutines.runBlocking // 🟢 1. IMPORT THIS
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.web.client.RestTemplate

@SpringBootApplication(scanBasePackages = ["com.tsd"])
// Note: Since you have MultiDataSourceConfig, you technically don't need these two lines below
// (the config file handles it), but keeping them is usually harmless if packages match.
@EnableJpaRepositories(basePackages = ["com.tsd"])
@EntityScan(basePackages = ["com.tsd"])
class TsdApplication {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun runner(engine: EnterpriseWorkflowEngine): CommandLineRunner {
        return CommandLineRunner {
            println("\n------------------------------------------------------------")
            println(" SYSTEM ONLINE. Engine Component Loaded: ${engine.javaClass.simpleName}")
            println(" WAITING FOR SYNC: Workflow execution paused until platform files are ready.")
            println("------------------------------------------------------------\n")

            // 🟢 2. FIX: Wrap the suspend function in runBlocking
            runBlocking {
                try {
                    engine.executeWorkflow("TSD", "TSD-01")
                } catch (e: Exception) {
                    println("❌ WORKFLOW FAILED TO START: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<TsdApplication>(*args)
}