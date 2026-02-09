package com.tsd.app.account.web

import com.tsd.platform.spi.WorkflowEngine
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/registry")
class RegistryController(
    private val engine: WorkflowEngine
) {

    @GetMapping("/run")
    fun runWorkflow(): String {
        val jobId = "REG-" + UUID.randomUUID().toString().substring(0, 8)
        val data = mapOf("Source" to "RegistryAPI")

        // 🟢 FIX: Use executeJob
        engine.executeJob(jobId, data)

        return "✅ Workflow Triggered! Job: $jobId"
    }
}