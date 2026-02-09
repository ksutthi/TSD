package com.tsd.app.event.cartridge

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import com.tsd.platform.engine.util.EngineAnsi

@Component("Set_Cash_Dividend_Rate")
class SetCashDividendRateCartridge : Cartridge {
    override val id = "Set_Cash_Dividend_Rate"
    override val version = "1.0"
    override val priority = 10

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 🟢 1. Get Dynamic Prefix
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        print(EngineAnsi.GRAY + "   💵 $prefix Looking up Dividend Rate..." + EngineAnsi.RESET)
        println("")

        // Let's use a DIFFERENT rate to prove M1 updates automatically!
        val approvedRate = 4.00

        // WRITE to packet (So M1 can read it later)
        packet.data["Dividend_Rate"] = approvedRate

        // 🟢 2. ALIGNMENT FIX: 6 Spaces outer + 6 Spaces inner (Matches J1)
        println("      " + EngineAnsi.GREEN + "    ✅ $prefix Rate Set: $approvedRate THB per share" + EngineAnsi.RESET)
    }

    override fun shutdown() {}
}