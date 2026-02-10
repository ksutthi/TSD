package com.tsd.app.authorization.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.util.SecretContext
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Random

@Component("CFO_Signoff_Sim")
class CfoSignoffSimCartridge : Cartridge {

    override val id = "CFO_Signoff_Sim"
    override val version = "1.0"
    // Prio 1 ensures it runs in the same phase as Identity Mgmt
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[N1]"
        println(EngineAnsi.CYAN + "      👔 $prefix CFO Robot: Reviewing transaction..." + EngineAnsi.RESET)

        // 1. Get Money from Vault (Consensus needs reliable data!)
        val accountIdStr = context.getString("Account_ID")
        val accountId = accountIdStr?.toLongOrNull()
        var amount = BigDecimal.ZERO

        if (accountId != null) {
            val secretMoney = SecretContext.withdraw(accountId)
            if (secretMoney != null) {
                amount = secretMoney
            }
        }

        // 2. CFO Logic: If amount > 100k, he flips a coin
        val riskThreshold = BigDecimal("100000.00")
        val isHighRisk = amount > riskThreshold

        if (isHighRisk) {
            println(EngineAnsi.YELLOW + "      👔 $prefix CFO: This is a High Value amount ($amount). Checking my schedule..." + EngineAnsi.RESET)

            // Simulate "Human" Delay
            try { Thread.sleep(200) } catch (e: InterruptedException) {}

            // 90% Approval Rate (Random Logic)
            val mood = Random().nextInt(10)
            if (mood > 0) {
                println(EngineAnsi.GREEN + "      ✅ $prefix CFO: Approved. (Stamped)" + EngineAnsi.RESET)
                // 🟢 STAMP THE PACKET
                packet.data["CFO_APPROVAL"] = "YES"
            } else {
                println(EngineAnsi.RED + "      ⛔ $prefix CFO: REJECTED! I don't like this Account." + EngineAnsi.RESET)
                // Mark as rejected
                packet.data["CFO_APPROVAL"] = "NO"
                throw RuntimeException("CFO Refused to Sign!")
            }
        } else {
            println(EngineAnsi.GREEN + "      ✅ $prefix CFO: Small change. Auto-signed." + EngineAnsi.RESET)
            packet.data["CFO_APPROVAL"] = "YES"
        }
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}