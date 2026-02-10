package com.tsd.app.authorization.cartridge

import com.tsd.app.authorization.service.OneIdProxy
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.util.SecretContext
import com.tsd.platform.exception.JobSuspensionException
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.persistence.SuspendedActionRepository
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal

@Component("Flag_High_Value_Auth")
class CallIdentityMgmtCartridge(
    private val oneIdProxy: OneIdProxy,
    private val suspensionRepo: SuspendedActionRepository,
    private val cfoRobot: CfoSignoffSimCartridge // 🟢 1. INJECT THE ROBOT
) : Cartridge {

    override val id = "Call_Identity_Mgmt"
    override val version = "1.0"
    override val priority = 1

    private var limit: BigDecimal = BigDecimal("50000000.00")

    override fun initialize(context: KernelContext) {
        loadAmlConfig()
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 1. Get the Prefix for logging
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        // 🕵️ TRACE LOG
        val keys = packet.data.keys.joinToString(", ")
        println(EngineAnsi.CYAN + "      📦 [Auth_Engine] Packet Keys ON ARRIVAL: [$keys]" + EngineAnsi.RESET)
        println(EngineAnsi.CYAN + "-------------------------------------------------------------" + EngineAnsi.RESET)

        // 2. Strict Read: Try Context first (Short-term memory)
        var amount = context.getAmount("Net_Amount")

        // 3. 🟢 ROBUST RECOVERY: If Context is empty (0), check the Packet (Long-term memory)
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            val rawValue = packet.data["Net_Amount"]
            // Debug: See exactly what is in the packet
            println(EngineAnsi.YELLOW + "📦 [DEBUG] Packet Probe: Found '$rawValue' (Type: ${rawValue?.javaClass?.simpleName})" + EngineAnsi.RESET)

            if (rawValue != null) {
                amount = when (rawValue) {
                    is BigDecimal -> rawValue
                    is Double -> BigDecimal.valueOf(rawValue)
                    is Int -> BigDecimal(rawValue)
                    is String -> if (rawValue.isNotBlank()) BigDecimal(rawValue) else BigDecimal.ZERO
                    else -> BigDecimal.ZERO
                }
                println(EngineAnsi.GREEN + "      ♻️ [RECOVERY] Success! Converted Packet data to: $amount" + EngineAnsi.RESET)
            }
        }

        // 4. 🟢 TELEPORT RECOVERY: If Packet failed, Check the Secret Vault!
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            val accountIdStr = context.getString("Account_ID")
            val accountId = accountIdStr.toLongOrNull()

            if (accountId != null) {
                val secretMoney = SecretContext.withdraw(accountId)
                if (secretMoney != null) {
                    amount = secretMoney
                    println(EngineAnsi.BOLD_GREEN + "      🔓 [Secret_Vault] RECOVERY SUCCESS! Retrieved $amount for Account $accountId" + EngineAnsi.RESET)
                }
            }
        }

        println(EngineAnsi.CYAN + "-------------------------------------------------------------" + EngineAnsi.RESET)

        // 5. CRASH DIAGNOSTICS: If it is STILL zero, we must stop.
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            println(EngineAnsi.RED + "      ❌ [CRITICAL] Net_Amount is ZERO (Missing)! \n      Context Dump: $context" + EngineAnsi.RESET)
            throw RuntimeException("❌ $prefix CRITICAL: Net_Amount is 0 or Missing! Cannot perform Auth check.")
        }

        // 🟢 DEBUG SUCCESS
        println(EngineAnsi.CYAN + "      💰 [DEBUG] Success! Verified Net_Amount: $amount" + EngineAnsi.RESET)

        // 6. Get 'Account_ID'
        val accountIdStr = context.getString("Account_ID")
        val accountId = accountIdStr.toLongOrNull()
            ?: throw RuntimeException("❌ $prefix CRITICAL: Account_ID missing from Context!")

        // 7. Log the Start
        println(EngineAnsi.GRAY + "         ... 📞 $prefix Calling Identity/Auth Service (Checking: $amount vs Limit: $limit) ..." + EngineAnsi.RESET)

        // 8. RESUME CHECK (Skip if already approved)
        val ticket = suspensionRepo.findByAccountIdAndCartridgeName(accountId, id)
        if (ticket != null && ticket.status == "APPROVED") {
            println("         " + EngineAnsi.BOLD_GREEN + "✅ $prefix [RESUME] Found Approved Ticket! Bypassing Limit Check." + EngineAnsi.RESET)
            // Note: Even if resumed, we might want to check consensus, but usually resume implies full pass.
            // For this demo, let's proceed to consensus even on resume or skip it.
            // Let's assume Resume skips EVERYTHING.
            return
        }

        // 9. NORMAL CHECK (The High Value Logic)
        if (amount > limit) {
            println("         " + EngineAnsi.RED + "✖ $prefix CRASHED : $id - Amount $amount exceeds limit ($limit)." + EngineAnsi.RESET)
            throw JobSuspensionException(
                reason = "Amount $amount exceeds auto-approval limit ($limit).",
                suspenseCode = "WAIT_MANUAL_AUTH"
            )
        }

        // 10. EXTERNAL CALL (OneID)
        val user = "user_$accountId"
        try {
            val isAllowed = oneIdProxy.checkAccess(user)
            if (!isAllowed) {
                throw RuntimeException("OneID Service Denied Access for user: $user")
            }
            println("         " + EngineAnsi.GREEN + "✅ $prefix Identity Verified for $user" + EngineAnsi.RESET)

            // --- 11. 🟢 TRIGGER THE CFO ROBOT (Manual Consensus) ---
            // We force the CFO robot to run right now because the framework didn't call him.
            println(EngineAnsi.CYAN + "      📞 $prefix Summoning CFO for Consensus Check..." + EngineAnsi.RESET)
            cfoRobot.execute(packet, context)

            // --- 12. 🟢 FINAL CONSENSUS VERDICT ---
            val cfoStatus = packet.data["CFO_APPROVAL"]

            if (cfoStatus == "YES") {
                println(EngineAnsi.BOLD_GREEN + "      🤝 [CONSENSUS] VICTORY! Both Parties Agreed (OneID + CFO)" + EngineAnsi.RESET)
            } else {
                throw RuntimeException("❌ [CONSENSUS] FAILED! CFO rejected the transaction.")
            }

        } catch (e: Exception) {
            // Unwrap exception if it came from the robot
            throw RuntimeException("Auth/Consensus Error: ${e.message}")
        }

        println(EngineAnsi.RED + "      🐞 [DEBUG_TEST] I AM THE NEW AUTH CODE! Verification finished for amount: $amount" + EngineAnsi.RESET)
    }

    private fun loadAmlConfig() {
        val filePath = "/config/cartridges/99_business_params.csv"
        try {
            val inputStream = javaClass.getResourceAsStream(filePath)
            if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // Skip header
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
                println("      ⚠️ [Identity_Mgmt] Config file not found: $filePath (Using default 50M)")
            }
        } catch (e: Exception) {
            println("      ❌ [Identity_Mgmt] Error loading config: ${e.message}")
        }
    }

    override fun shutdown() {}
}