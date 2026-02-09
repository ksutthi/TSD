package com.tsd.app.calculation.cartridge

import com.tsd.app.calculation.service.DividendCalculation
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class CalcGrossBenefitCartridge(
    private val dividendCalculation: DividendCalculation // 💉 Inject the Logic
) : Cartridge {
    override val id = "Calc_Gross_Benefit"
    override val version = "1.0"
    override val priority = 2

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 1. Get Raw Data
        val qty = dividendCalculation.safeBigDecimal(packet.data["Share_Balance"]) // 👈 Match Batch Config
        val rate = dividendCalculation.safeBigDecimal(packet.data["Rate"])

        // 2. Use Service for Math
        val gross = dividendCalculation.calculateGross(qty, rate)

        // 3. Save Result
        packet.data["Gross_Amount"] = gross
        println("      🧮 [M2] Gross: $qty x $rate = $gross")
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}