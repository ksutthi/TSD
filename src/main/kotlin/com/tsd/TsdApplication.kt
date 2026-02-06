package com.tsd

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.client.RestTemplate

@SpringBootApplication
// Scans App, Platform, and Integration
@ComponentScan(basePackages = ["com.tsd"])
class TsdApplication {

    // 🟢 FIX: Create the missing RestTemplate so OneIdProxy can use it
    @Bean
    fun runner(jobLauncher: org.springframework.batch.core.launch.JobLauncher,
               job: org.springframework.batch.core.Job): org.springframework.boot.CommandLineRunner {
        return org.springframework.boot.CommandLineRunner {
            val uniqueParams = org.springframework.batch.core.JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis()) // 🟢 UNIQUE ID
                .toJobParameters()

            jobLauncher.run(job, uniqueParams)
        }
    }
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}

fun main(args: Array<String>) {
    runApplication<TsdApplication>(*args)
}