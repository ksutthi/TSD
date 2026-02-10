package com.tsd.app.calculation.cartridge

import com.tsd.app.calculation.service.DividendCalculation
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import com.tsd.platform.engine.util.EngineAnsi

@Component("Calc_Gross_Benefit")
class CalcGrossBenefitCartridge(
    private val dividendCalculation: DividendCalculation
) : Cartridge {
    override val id = "Calc_Gross_Benefit"
    override val version = "3.0" // 🟢 Bump to 3.0 (The "Shotgun" Release)
    override val priority = 2

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val qty = context.getAmount("Share_Balance")
        val rate = context.getAmount("Rate")

        val gross = dividendCalculation.calculateGross(qty, rate)

        // 🟢 FIX 1: Save to CONTEXT (Modern way)
        context.set("Gross_Amount", gross)
        context.set("Net_Amount", gross)

        // 🟢 FIX 2: Save to PACKET (Legacy way - for the Tax Engine)
        packet.data["Gross_Amount"] = gross
        packet.data["Net_Amount"] = gross

        // Debug Log
        println(EngineAnsi.MAGENTA + "==================================================" + EngineAnsi.RESET)
        println(EngineAnsi.MAGENTA + " 💰 [v3.0] SAVED EVERYWHERE: $gross" + EngineAnsi.RESET)
        println(EngineAnsi.MAGENTA + "    - Context: ✅" + EngineAnsi.RESET)
        println(EngineAnsi.MAGENTA + "    - Packet:  ✅" + EngineAnsi.RESET)
        println(EngineAnsi.MAGENTA + "==================================================" + EngineAnsi.RESET)
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}