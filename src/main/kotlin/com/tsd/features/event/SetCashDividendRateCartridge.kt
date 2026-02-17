package com.tsd.features.event

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import com.tsd.platform.engine.util.EngineAnsi

@Component("Set_Cash_Dividend_Rate")
class SetCashDividendRateCartridge : Cartridge {
    override val id = "Set_Cash_Dividend_Rate"
    override val version = "1.0"
    override val priority = 10

    override fun initialize(context: ExecutionContext) {}

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // 🟢 1. Get Dynamic Prefix
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        print(EngineAnsi.GRAY + "   💵 $prefix Looking up Dividend Rate..." + EngineAnsi.RESET)
        println("")

        // Let's use a DIFFERENT rate to prove M1 updates automatically!
        val approvedRate = 4.00

        // ❌ OLD: packet.data["Dividend_Rate"] = approvedRate (Engine can't see this!)

        // ✅ NEW: Write to Context (So the Engine can persist it!)
        // We save it as "Rate" directly so the Calculator finds it easily.
        context.set("Rate", approvedRate)
        context.set("Dividend_Rate", approvedRate) // Save alias just in case

        // 🟢 2. ALIGNMENT FIX: 6 Spaces outer + 6 Spaces inner (Matches J1)
        println("      " + EngineAnsi.GREEN + "    ✅ $prefix Rate Set: $approvedRate THB per share" + EngineAnsi.RESET)
    }

    override fun shutdown() {}
}