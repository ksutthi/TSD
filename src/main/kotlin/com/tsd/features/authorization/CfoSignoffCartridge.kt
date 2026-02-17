package com.tsd.features.authorization

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Random

@Component("CFO_Signoff_Sim")
class CfoSignoffCartridge : Cartridge {

    override val id = "CFO_Signoff_Sim"
    override val version = "2.0" // Version Bump for Clean Code
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // 🟢 FIX 1: Clean Prefix handling (No warnings)
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[N1]"

        println(EngineAnsi.CYAN + "      👔 $prefix CFO Robot: Reviewing transaction ${packet.id}..." + EngineAnsi.RESET)

        // 🟢 FIX 2: Standardized Amount Retrieval (Matches Bank Cartridge)
        // We get the money from the Packet, not SecretContext (Legacy)
        val rawAmount = packet.data["Net_Amount"]
        val amount: BigDecimal = when (rawAmount) {
            is BigDecimal -> rawAmount
            is Double -> BigDecimal.valueOf(rawAmount)
            is String -> BigDecimal(rawAmount)
            else -> BigDecimal.ZERO
        }

        // 2. CFO Logic: If amount > 100k, he checks deeper
        val riskThreshold = BigDecimal("100000.00")
        val isHighRisk = amount > riskThreshold

        if (isHighRisk) {
            println(EngineAnsi.YELLOW + "      👔 $prefix CFO: High Value ($amount THB). Checking risk matrix..." + EngineAnsi.RESET)

            // Simulate "Human" Delay
            try { Thread.sleep(200) } catch (e: InterruptedException) {}

            // 90% Approval Rate
            val mood = Random().nextInt(10)
            if (mood > 0) {
                println(EngineAnsi.GREEN + "      ✅ $prefix CFO: Approved. (Stamped)" + EngineAnsi.RESET)
                packet.data["CFO_APPROVAL"] = "YES"
            } else {
                println(EngineAnsi.RED + "      ⛔ $prefix CFO: REJECTED! Risk too high." + EngineAnsi.RESET)
                packet.data["CFO_APPROVAL"] = "NO"
                throw RuntimeException("CFO Refused to Sign!")
            }
        } else {
            println(EngineAnsi.GREEN + "      ✅ $prefix CFO: Small change ($amount THB). Auto-signed." + EngineAnsi.RESET)
            packet.data["CFO_APPROVAL"] = "YES"
        }
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}