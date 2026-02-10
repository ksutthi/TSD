package com.tsd.platform.engine.loader

import com.tsd.platform.config.ConfigMatrix
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.MatrixRule
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import jakarta.annotation.PostConstruct
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component
class WorkflowLoader(
    private val cartridges: List<Cartridge>,
    private val configMatrix: ConfigMatrix
) {
    // Standard Paths
// ✅ NEW: Loads ONLY the correct file
    private val WORKFLOW_PATH = "classpath*:config/workflows/workflow_matrix.csv"
    val rules = mutableListOf<MatrixRule>()

    @PostConstruct
    fun init() {
        println(EngineAnsi.CYAN + "--------------------------------------------------" + EngineAnsi.RESET)
        initializeCartridges()
        printCartridgeManifest()
        loadEnterpriseMatrix() // 🟢 Now prints Tree + Conditions
        println(EngineAnsi.CYAN + "--------------------------------------------------" + EngineAnsi.RESET)
    }

    private fun initializeCartridges() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] 🔌 Initializing Cartridges..." + EngineAnsi.RESET)
        val startupContext = KernelContext(jobId = "STARTUP", tenantId = "SYSTEM")
        cartridges.forEach { try { it.initialize(startupContext) } catch (e: Exception) {} }
    }

    private fun printCartridgeManifest() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] Linking Cartridges..." + EngineAnsi.RESET)
        println("   🎉 Total Cartridges Discovered: ${cartridges.size}")

        cartridges.sortedBy { it.priority }.forEachIndexed { index, cartridge ->
            val number = "${index + 1}.".padEnd(4)
            val name = cartridge.id.padEnd(35)
            val version = "v${cartridge.version}".padEnd(8)
            val prio = "Prio:${cartridge.priority}".padEnd(8)

            println("      " + EngineAnsi.GREEN + number + "✨ " + EngineAnsi.WHITE + name +
                    EngineAnsi.GRAY + version +
                    EngineAnsi.YELLOW + prio + EngineAnsi.RESET)
        }
        println("")
    }

    private fun loadEnterpriseMatrix() {
        println(EngineAnsi.CYAN + " [WorkflowLoader] 📂 Loading Enterprise Workflows (Modern Format)..." + EngineAnsi.RESET)
        val resolver = PathMatchingResourcePatternResolver()
        rules.clear()

        try {
            val resources = resolver.getResources(WORKFLOW_PATH)
            if (resources.isEmpty()) {
                println(EngineAnsi.RED + "   ❌ ERROR: No Matrix files found!" + EngineAnsi.RESET)
                return
            }

            for (resource in resources) {
                BufferedReader(InputStreamReader(resource.inputStream)).use { reader ->
                    reader.lineSequence()
                        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("Module_ID") }
                        .forEach { line ->
                            val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().removeSurrounding("\"") }

                            if (parts.size >= 9) {
                                val modId = parts[0]
                                val beanName = parts[6]

                                val rule = MatrixRule(
                                    moduleId = modId,
                                    moduleName = parts[1],
                                    slotId = parts[2],
                                    slotName = parts[3],
                                    stepId = parts[4],
                                    cartridgeId = parts[5],
                                    cartridgeName = beanName,
                                    strategy = parts[7],
                                    selector = parts[8],
                                    scope = if (modId == "J") "JOB" else "ITEM"
                                )
                                rules.add(rule)
                            }
                        }
                }
            }

            println("   ✅ SUCCESS       : Loaded ${rules.size} Modern Matrix Rules.")
            println(EngineAnsi.CYAN + "   🌳 Workflow Tree Visualization:" + EngineAnsi.RESET)

            // 🟢 HIERARCHICAL PRINTING LOGIC 🟢

            val modules = rules.groupBy { it.moduleId to it.moduleName }

            modules.forEach { (modPair, modRules) ->
                val (modId, modName) = modPair
                val scope = modRules.first().scope

                // Level 1: Module
                println("")
                println("      " + EngineAnsi.BLUE + "📦 [$modId] $modName" + EngineAnsi.GRAY + " (Scope: $scope)" + EngineAnsi.RESET)

                val slots = modRules.groupBy { it.slotId to it.slotName }

                slots.forEach { (slotPair, slotRules) ->
                    val (slotId, slotName) = slotPair

                    // Level 2: Slot
                    println("      " + EngineAnsi.GRAY + "    └─ " + EngineAnsi.YELLOW + "⚙️ [$slotId] $slotName" + EngineAnsi.RESET)

                    slotRules.forEach { rule ->
                        val stepInfo = "[S${rule.stepId}]"
                        val cartridge = rule.cartridgeName
                        val strat = if (rule.strategy == "PARALLEL") "⚡ PARALLEL" else "SERIAL"

                        // Level 3: Step
                        println("      " + EngineAnsi.GRAY + "         └─ " + EngineAnsi.GREEN + "$stepInfo $cartridge" + EngineAnsi.WHITE + " [$strat]" + EngineAnsi.RESET)

                        // 🟢 Level 4: Condition (Selector)
                        // Only print if there is a real condition
                        if (rule.selector.isNotBlank() && rule.selector != "*") {
                            println("      " + EngineAnsi.GRAY + "              👉 Cond: " + EngineAnsi.CYAN + rule.selector + EngineAnsi.RESET)
                        }
                    }
                }
            }
            println("")

            configMatrix.loadRules(rules)

        } catch (e: Exception) {
            println(EngineAnsi.RED + "   🔥 CRITICAL FAIL: ${e.message}" + EngineAnsi.RESET)
            e.printStackTrace()
        }
    }
}