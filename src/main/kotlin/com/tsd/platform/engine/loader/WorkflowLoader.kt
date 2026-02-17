package com.tsd.platform.engine.loader

import com.tsd.platform.config.ConfigMatrix
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.model.MatrixRule
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import com.tsd.platform.engine.state.KernelContext

@Component
class WorkflowLoader(
    private val cartridges: List<Cartridge>,
    private val configMatrix: ConfigMatrix
) {
    private val workflowPath = "classpath*:config/workflows/workflow_matrix.csv"
    val rules = mutableListOf<MatrixRule>()

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
        cartridges.forEach { try { it.initialize(startupContext) } catch (_: Exception) {} }
    }

    private fun printCartridgeManifest() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] Linking Cartridges..." + EngineAnsi.RESET)
        println("   🎉 Total Cartridges Discovered: ${cartridges.size}")

        cartridges.sortedBy { it.priority }.forEachIndexed { index, cartridge ->
            val number = "${index + 1}.".padEnd(4)
            val name = cartridge.id.padEnd(35)
            val version = "v${cartridge.version}".padEnd(8)
            val priorityStr = "Priority:${cartridge.priority}".padEnd(8)

            println("      " + EngineAnsi.GREEN + number + "✨ " + EngineAnsi.WHITE + name +
                    EngineAnsi.GRAY + version +
                    EngineAnsi.YELLOW + priorityStr + EngineAnsi.RESET)
        }
        println("")
    }

    private fun loadEnterpriseMatrix() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] 📂 Loading Enterprise Workflows (Multi-Tenant)..." + EngineAnsi.RESET)
        val resolver = PathMatchingResourcePatternResolver()
        rules.clear()

        try {
            val resources = resolver.getResources(workflowPath)
            if (resources.isEmpty()) {
                println(EngineAnsi.RED + "   ❌ ERROR: No Matrix files found!" + EngineAnsi.RESET)
                return
            }

            for (resource in resources) {
                BufferedReader(InputStreamReader(resource.inputStream)).use { reader ->
                    reader.lineSequence()
                        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("Registrar_Code") }
                        .forEach { line ->
                            // Regex splits by comma ONLY if not inside quotes
                            val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                                .map { it.trim().removeSurrounding("\"") }

                            if (parts.size >= 12) {
                                val rule = MatrixRule(
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

                                    // 🟢 NEW: Read Column 13 for Config JSON (if exists)
                                    // Also replaces '' with " to handle CSV escaping
                                    configJson    = if (parts.size >= 13) parts[12].replace("''", "\"") else "{}"
                                )
                                rules.add(rule)
                            }
                        }
                }
            }

            println("   ✅ SUCCESS       : Loaded ${rules.size} Modern Matrix Rules.")
            println(EngineAnsi.CYAN + "   🌳 Workflow Tree Visualization:" + EngineAnsi.RESET)
            printVisualization()

            configMatrix.loadRules(rules)

        } catch (e: Exception) {
            println(EngineAnsi.RED + "   🔥 CRITICAL FAIL: ${e.message}" + EngineAnsi.RESET)
            e.printStackTrace()
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

                        // Optional: Print config if it exists
                        if (rule.configJson != "{}") {
                            println("      " + EngineAnsi.GRAY + "              🔧 Config: " + rule.configJson + EngineAnsi.RESET)
                        }
                    }
                }
            }
        }
        println("")
    }
}