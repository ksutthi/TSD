package com.tsd

import com.tsd.platform.engine.core.EnterpriseWorkflowEngine
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate

@SpringBootApplication
class TsdApplication {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    fun runner(engine: EnterpriseWorkflowEngine): CommandLineRunner {
        return CommandLineRunner {
            // 🟢 FIX: We "use" the engine parameter here to silence the warning
            println("\n------------------------------------------------------------")
            println(" SYSTEM ONLINE. Engine Component Loaded: ${engine.javaClass.simpleName}")
            println(" WAITING FOR SYNC: Workflow execution paused until platform files are ready.")
            println("------------------------------------------------------------\n")

            // TODO: Uncomment this after syncing 'platform/engine'
            // engine.executeWorkflow("TSD", "TSD-01")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<TsdApplication>(*args)
}