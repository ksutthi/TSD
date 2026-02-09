package com.tsd.app.eligibility.cartridge

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import java.math.BigDecimal
import com.tsd.platform.engine.util.EngineAnsi
import org.springframework.stereotype.Component

@Component("Sanity_Check_Positions")
class SanityCheckPositionsCartridge : Cartridge {

    override val id: String = "Sanity_Check_Positions"
    override val version: String = "1.0"
    override val priority: Int = 20 // Runs AFTER Ingestion (Priority 10)

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 🟢 1. Get Dynamic Prefix (e.g., [K1-2])
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        print(EngineAnsi.GRAY + "   🧐 $prefix Verifying Position Integrity..." + EngineAnsi.RESET)
        println("")

        // 2. Retrieve the data
        val rawBalance = packet.data["Balance"] ?: packet.data["Share_Balance"]

        // 3. Convert to BigDecimal
        val balance = when (rawBalance) {
            is BigDecimal -> rawBalance
            is Number -> BigDecimal(rawBalance.toString())
            is String -> BigDecimal(rawBalance)
            else -> BigDecimal.ZERO
        }

        // 4. Logic Check
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            // 🟢 ALIGNMENT FIX: 5 Spaces inside
            println("    " + EngineAnsi.RED + "     ⚠️ $prefix ALARM: Negative Balance Detected! ($balance)" + EngineAnsi.RESET)
            packet.data["Eligibility_Status"] = "REJECTED"
        } else if (balance.compareTo(BigDecimal.ZERO) == 0) {
            // 🟢 ALIGNMENT FIX: 5 Spaces inside
            println("    " + EngineAnsi.YELLOW + "     ⚠️ $prefix Warning: Zero Balance. Shareholder has no position." + EngineAnsi.RESET)
            packet.data["Eligibility_Status"] = "SKIPPED"
        } else {
            // 🟢 ALIGNMENT FIX: 5 Spaces inside (Total 9 spaces + 3 chars for '✅ ' = 12 chars offset)
            println("    " + EngineAnsi.GREEN + "      ✅ $prefix Position Verified: $balance shares are valid." + EngineAnsi.RESET)
            packet.data["Eligibility_Status"] = "APPROVED"
        }
    }
    override fun shutdown() {}
}