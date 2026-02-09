package com.tsd.app.calculation.cartridge

import com.tsd.app.calculation.service.DividendCalculation
import com.tsd.platform.engine.util.EngineAnsi // 🟢 ADDED: For Colors
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.math.BigDecimal

// 🟢 FIX 1: Match the name in your CSV (Apply_WHT_Standard)
@Component("Apply_WHT_Standard")
class ApplyWhtStandardCartridge(
    private val dividendCalculation: DividendCalculation
) : Cartridge {
    override val id = "Apply_WHT_Standard"
    override val version = "1.0"
    override val priority = 4

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 🟢 FIX 2: Get Dynamic Step ID (e.g. [M2-S1])
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        // 1. Get Data
        val gross = dividendCalculation.safeBigDecimal(packet.data["Gross_Amount"])

        // Default tax to 10% if not found
        val taxRateRaw = dividendCalculation.safeBigDecimal(packet.data["WHT_Rate"])
        val taxRate = if (taxRateRaw > BigDecimal.ZERO) taxRateRaw else BigDecimal("0.10")

        // 2. Use Service for Math
        val tax = dividendCalculation.calculateTax(gross, taxRate)
        val net = dividendCalculation.calculateNet(gross, tax)

        // 3. Save Result
        packet.data["Tax_Amount"] = tax
        packet.data["Net_Amount"] = net

        // 🟢 FIX 3: Use $prefix, Yellow Color, and 6-space alignment
        val percentage = taxRate.multiply(BigDecimal(100)).toInt()

        println("      " + EngineAnsi.YELLOW + "💸 $prefix Tax ($percentage%): -$tax | Net: $net THB" + EngineAnsi.RESET)
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}