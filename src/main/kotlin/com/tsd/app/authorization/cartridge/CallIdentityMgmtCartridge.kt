package com.tsd.app.authorization.cartridge

import com.tsd.app.authorization.service.OneIdProxy
import com.tsd.platform.engine.state.JobAccumulator // 🟢 IMPORT
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal

@Component("Flag_High_Value_Auth")
class CallIdentityMgmtCartridge(
    private val oneIdProxy: OneIdProxy,
    private val memory: JobAccumulator // 🟢 INJECT BEAN
) : Cartridge {

    override val id = "Call_Identity_Mgmt"
    override val version = "4.0" // Enterprise Bean Version
    override val priority = 1

    private var limit: BigDecimal = BigDecimal("50000000.00")

    override fun initialize(context: KernelContext) {
        loadAmlConfig()
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = "[N1-SPRING]"
        println(EngineAnsi.CYAN + "      🛡️ $prefix Scanning Spring Memory for High Value Transactions (> $limit)..." + EngineAnsi.RESET)

        // 🟢 1. Read from Spring Bean (Partitioned by Job ID)
        val allAccounts = memory.getAllPayouts(context.jobId)

        var highValueCount = 0
        var rejectedCount = 0

        if (allAccounts.isEmpty()) {
            println("         ⚠️ No accounts found in Spring Memory.")
            return
        }

        // 2. Loop & Check
        allAccounts.forEach { (accountId, amount) ->
            if (amount > limit) {
                highValueCount++
                print("         🔍 $prefix Reviewing Account $accountId ($amount THB)... ")

                // 3. Authorization
                val authorized = performAuthCheck(accountId)

                if (authorized) {
                    println(EngineAnsi.GREEN + "APPROVED ✅" + EngineAnsi.RESET)
                } else {
                    println(EngineAnsi.RED + "REJECTED ❌ (Removed from Payout)" + EngineAnsi.RESET)

                    // 🔫 REMOVE from Spring Memory (So N3 doesn't pay it)
                    memory.removePayout(context.jobId, accountId)
                    rejectedCount++
                }
            }
        }

        if (highValueCount == 0) {
            println("         ✅ $prefix No high value transactions found.")
        } else {
            println(EngineAnsi.CYAN + "      📊 $prefix Scan Complete. High Value: $highValueCount, Rejected: $rejectedCount" + EngineAnsi.RESET)
        }
    }

    private fun performAuthCheck(accountId: Long): Boolean {
        val user = "user_$accountId"
        return try {
            oneIdProxy.checkAccess(user)
        } catch (e: Exception) {
            false
        }
    }

    private fun loadAmlConfig() {
        val filePath = "/config/cartridges/99_business_params.csv"
        try {
            val inputStream = javaClass.getResourceAsStream(filePath)
            if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine()
                    reader.forEachLine { line ->
                        val parts = line.split(",").map { it.trim() }
                        if (parts.size >= 5 && parts[1] == "AML" && parts[3] == "AML_THRESHOLD") {
                            val valueStr = parts[4]
                            limit = if (valueStr.isNotBlank()) BigDecimal(valueStr) else BigDecimal("50000000.00")
                            println("      📋 [Identity_Mgmt] Loaded Config: Limit = $limit")
                        }
                    }
                }
            } else {
                println("      ⚠️ [Identity_Mgmt] Config file not found. Using Default: $limit")
            }
        } catch (e: Exception) {
            println("      ❌ [Identity_Mgmt] Error loading config: ${e.message}")
        }
    }

    override fun shutdown() {}
}