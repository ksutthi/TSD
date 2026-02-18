package com.tsd.platform.config.loader

import com.tsd.platform.config.ConfigMatrix
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.model.MatrixRule
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.engine.state.KernelContext
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component
class WorkflowLoader(
    private val cartridges: List<Cartridge>,
    private val configMatrix: ConfigMatrix
) {
    // 🟢 GLOBAL SEARCH: Finds ALL CSV files anywhere in the project
    // We will filter for the right ones inside the logic below.
    private val workflowPath = "classpath*:**/*.csv"

    val rules = mutableListOf<MatrixRule>()
    private val logger = LoggerFactory.getLogger(WorkflowLoader::class.java)

    @PostConstruct
    fun init() {
        println(EngineAnsi.CYAN + "--------------------------------------------------" + EngineAnsi.RESET)
        initializeCartridges()
        printCartridgeManifest()
        loadEnterpriseMatrix()
        println(EngineAnsi.CYAN + "--------------------------------------------------" + EngineAnsi.RESET)
    }

    private fun initializeCartridges() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] 🔌 Initializing Cartridges..." + EngineAnsi.RESET)
        val startupContext = KernelContext(jobId = "STARTUP", tenantId = "SYSTEM")
        cartridges.forEach {
            try {
                it.initialize(startupContext)
            } catch (e: Exception) {
                logger.warn("⚠️ Cartridge [${it.javaClass.simpleName}] initialization warning: ${e.message}")
            }
        }
    }

    private fun printCartridgeManifest() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] Linking Cartridges..." + EngineAnsi.RESET)
        println("   🎉 Total Cartridges Discovered: ${cartridges.size}")

        cartridges.sortedBy { it.priority }.forEachIndexed { index, cartridge ->
            val name = cartridge.javaClass.simpleName.padEnd(25)
            val version = cartridge.version.padEnd(6)
            val priority = cartridge.priority.toString()

            val color = when (cartridge.priority) {
                1 -> EngineAnsi.GREEN
                999 -> EngineAnsi.RED
                else -> EngineAnsi.YELLOW
            }

            println("      ${index + 1}.  $color✨ $name $version Priority:$priority" + EngineAnsi.RESET)
        }
        println("")
    }

    private fun loadEnterpriseMatrix() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] 📂 Loading Enterprise Workflows (Global Search)..." + EngineAnsi.RESET)
        val resolver = PathMatchingResourcePatternResolver()
        rules.clear()

        try {
            // 🟢 1. SCAN EVERYTHING
            val resources = resolver.getResources(workflowPath)

            var filesLoaded = 0

            for (resource in resources) {
                val filename = resource.filename ?: "Unknown"

                // 🟢 2. SMART FILTER: Only load our specific workflow files
                // This ignores '10_system.csv' or other random CSVs, but finds our files anywhere
                val isTargetFile = filename.contains("core_payments", ignoreCase = true) ||
                        filename.contains("chaos_scenarios", ignoreCase = true) ||
                        filename.contains("workflow_matrix", ignoreCase = true) // Fallback for old name

                if (!isTargetFile) continue

                println("   📄 Found Workflow File: " + EngineAnsi.YELLOW + filename + EngineAnsi.RESET + " (at ${resource.url})")
                filesLoaded++

                BufferedReader(InputStreamReader(resource.inputStream)).use { reader ->
                    reader.lineSequence()
                        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("Registrar_Code") }
                        .forEach { line -> parseRow(line, filename) }
                }
            }

            if (filesLoaded == 0) {
                println(EngineAnsi.RED + "   ❌ ERROR: Could not find 'core_payments.csv' or 'chaos_scenarios.csv' ANYWHERE." + EngineAnsi.RESET)
                println("      Please ensure they are in 'src/main/resources' (any subfolder).")
            } else {
                println("   ✅ SUCCESS       : Loaded ${rules.size} Rules from $filesLoaded Files.")
                printVisualization()
                configMatrix.loadRules(rules)
            }

        } catch (e: Exception) {
            println(EngineAnsi.RED + "   🔥 CRITICAL FAIL: ${e.message}" + EngineAnsi.RESET)
            e.printStackTrace()
        }
    }

    private fun parseRow(line: String, filename: String) {
        try {
            val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                .map { it.trim().removeSurrounding("\"") }

            if (parts.size >= 12) {
                rules.add(MatrixRule(
                    registrarCode = parts[0],
                    workflowId    = parts[1],
                    moduleId      = parts[2],
                    moduleName    = parts[3],
                    slotId        = parts[4],
                    slotName      = parts[5],
                    stepId        = parts[6].toIntOrNull() ?: 1,
                    cartridgeId   = parts[7],
                    cartridgeName = parts[8],
                    strategy      = parts[9],
                    selectorLogic = parts[10],
                    scope         = parts[11],
                    configJson    = if (parts.size >= 13) parts[12].replace("''", "\"") else "{}"
                ))
            } else {
                logger.warn("Skipping invalid row in $filename: $line")
            }
        } catch (e: Exception) {
            logger.error("Error parsing row in $filename: $line", e)
        }
    }

    private fun printVisualization() {
        val workflows = rules.groupBy { "${it.registrarCode} :: ${it.workflowId}" }

        workflows.forEach { (wfKey, wfRules) ->
            println("")
            println("   " + EngineAnsi.MAGENTA + "🏢 TENANT CONTEXT: [$wfKey]" + EngineAnsi.RESET)

            val modules = wfRules.groupBy { it.moduleId to it.moduleName }

            modules.forEach { (modPair, modRules) ->
                val (modId, modName) = modPair
                val scope = modRules.first().scope

                println("      " + EngineAnsi.BLUE + "📦 [$modId] $modName" + EngineAnsi.GRAY + " (Scope: $scope)" + EngineAnsi.RESET)

                val slots = modRules.groupBy { it.slotId to it.slotName }

                slots.forEach { (slotPair, slotRules) ->
                    val (slotId, slotName) = slotPair
                    println("      " + EngineAnsi.GRAY + "    └─ " + EngineAnsi.YELLOW + "⚙️ [$slotId] $slotName" + EngineAnsi.RESET)

                    slotRules.forEach { rule ->
                        val stepInfo = "[S${rule.stepId}]"
                        val cartridge = rule.cartridgeName
                        val strat = rule.strategy

                        println("      " + EngineAnsi.GRAY + "         └─ " + EngineAnsi.GREEN + "$stepInfo $cartridge" + EngineAnsi.WHITE + " [$strat]" + EngineAnsi.RESET)

                        if (rule.configJson != "{}" && rule.configJson.isNotBlank()) {
                            println("      " + EngineAnsi.GRAY + "              🔧 Config: " + rule.configJson + EngineAnsi.RESET)
                        }
                    }
                }
            }
        }
        println("")
    }
}