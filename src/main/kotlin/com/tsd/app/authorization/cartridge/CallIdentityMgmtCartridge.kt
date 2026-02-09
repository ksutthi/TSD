package com.tsd.app.authorization.cartridge

import com.tsd.app.authorization.service.OneIdProxy
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.exception.JobSuspensionException
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.persistence.SuspendedActionRepository
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

// 🟢 FIX 1: Map this class to the name used in your CSV
@Component("Flag_High_Value_Auth")
class CallIdentityMgmtCartridge(
    private val oneIdProxy: OneIdProxy,
    private val suspensionRepo: SuspendedActionRepository
) : Cartridge {

    override val id = "Call_Identity_Mgmt"
    override val version = "1.0"
    override val priority = 1

    private var limit: Double = 50_000_000.0

    override fun initialize(context: KernelContext) {
        loadAmlConfig()
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 🟢 FIX 2: Get the Dynamic Step ID (e.g. [N1-S1])
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        val amount = packet.data["Gross_Amount"]?.toString()?.toDoubleOrNull() ?: 0.0
        val accountId = packet.data["Account_ID"]?.toString()?.toLongOrNull() ?: 0L

        // 🟢 FIX 3: Use the Prefix and Color in your log
        print(EngineAnsi.GRAY + "         ... 📞 $prefix Calling Identity/Auth Service (Checking: $amount vs Limit: $limit) ..." + EngineAnsi.RESET)
        println("")

        // 1. RESUME CHECK
        val ticket = suspensionRepo.findByAccountIdAndCartridgeName(accountId, id)
        if (ticket != null && ticket.status == "APPROVED") {
            // 🟢 FIX 4: Success Message with Prefix
            println("         " + EngineAnsi.BOLD_GREEN + "✅ $prefix [RESUME] Found Approved Ticket! Bypassing Limit Check." + EngineAnsi.RESET)
            return
        }

        // 2. NORMAL CHECK
        if (amount > limit) {
            // 🟢 FIX 5: Error Message with Prefix
            println("         " + EngineAnsi.RED + "✖ $prefix CRASHED : $id - Amount $amount exceeds limit ($limit)." + EngineAnsi.RESET)

            throw JobSuspensionException(
                reason = "Amount $amount exceeds auto-approval limit ($limit).",
                suspenseCode = "WAIT_MANUAL_AUTH"
            )
        }

        // 3. EXTERNAL CALL
        val user = "user_$accountId"
        try {
            val isAllowed = oneIdProxy.checkAccess(user)
            if (!isAllowed) {
                throw RuntimeException("OneID Service Denied Access for user: $user")
            }
            // 🟢 FIX 6: Success Message with Prefix (Optional, if you want to see success logs)
            println("         " + EngineAnsi.GREEN + "✅ $prefix Identity Verified for $user" + EngineAnsi.RESET)

        } catch (e: Exception) {
            // We let the engine handle the exception, but we ensure the log above printed correctly first.
            throw RuntimeException("OneID Service Error: ${e.message}")
        }
    }

    private fun loadAmlConfig() {
        // I KEPT THIS LOGIC EXACTLY AS YOU HAD IT
        val filePath = "/config/cartridges/99_business_params.csv"
        try {
            val inputStream = javaClass.getResourceAsStream(filePath)
            if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine()
                    reader.forEachLine { line ->
                        val parts = line.split(",").map { it.trim() }
                        if (parts.size >= 5 && parts[1] == "AML" && parts[3] == "AML_THRESHOLD") {
                            limit = parts[4].toDoubleOrNull() ?: 50_000_000.0
                            println("      📋 [Identity_Mgmt] Loaded Config: Limit = $limit")
                        }
                    }
                }
            } else {
                println("      ⚠️ [Identity_Mgmt] Config file not found: $filePath (Using default 50M)")
            }
        } catch (e: Exception) {
            println("      ❌ [Identity_Mgmt] Error loading config: ${e.message}")
        }
    }

    override fun shutdown() {}
}