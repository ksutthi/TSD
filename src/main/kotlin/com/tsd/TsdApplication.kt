package com.tsd

import com.tsd.platform.engine.core.EnterpriseWorkflowEngine
import com.tsd.adapter.input.web.security.RegistrarParticipantDataIsolationSecurityConfig
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@Import(RegistrarParticipantDataIsolationSecurityConfig::class)
class TsdApplication {

    @Bean
    fun runner(engine: EnterpriseWorkflowEngine): CommandLineRunner {
        return CommandLineRunner {
            println("\n------------------------------------------------------------")
            println(" SYSTEM ONLINE. Engine Component Loaded: ${engine.javaClass.simpleName}")
            println(" WAITING FOR SYNC: Workflow execution paused until platform files are ready.")
            println("------------------------------------------------------------\n")

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