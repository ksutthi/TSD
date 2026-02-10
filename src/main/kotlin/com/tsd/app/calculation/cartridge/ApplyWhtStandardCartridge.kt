package com.tsd.app.calculation.cartridge

import com.tsd.app.calculation.service.DividendCalculation
import com.tsd.platform.engine.state.JobAccumulator // 🟢 IMPORT
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Apply_WHT_Standard")
class ApplyWhtStandardCartridge(
    private val dividendCalculation: DividendCalculation,
    private val memory: JobAccumulator // 🟢 INJECT
) : Cartridge {

    override val id = "Apply_WHT_Standard"
    override val version = "4.1" // Session-Aware Version
    override val priority = 4

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[M2]"

        // 1. Get Data
        val gross = context.getAmount("Gross_Amount")
        val taxRateRaw = context.getAmount("WHT_Rate")

        println("      " + EngineAnsi.CYAN + "🕵️‍♂️ $prefix [DEBUG] Reading from context: Gross=$gross" + EngineAnsi.RESET)

        // Default tax to 10%
        val taxRate = if (taxRateRaw > BigDecimal.ZERO) taxRateRaw else BigDecimal("0.10")

        // 2. Math Service
        val tax = dividendCalculation.calculateTax(gross, taxRate)
        val net = dividendCalculation.calculateNet(gross, tax)

        // 3. Save Result to Local Context (Short-term)
        context.set("Tax_Amount", tax)
        context.set("Net_Amount", net)
        packet.data["Net_Amount"] = net

        // 🟢 4. THE FIX: Save to the ACTIVE SESSION ID (Ignore the temporary Item Context ID)
        val accountIdStr = context.getString("Account_ID")
        val accountId = accountIdStr.toLongOrNull()

        if (accountId != null) {
            // 🔑 CRITICAL CHANGE: Use 'memory.getActiveJobId()' instead of 'context.jobId'
            val targetJobId = memory.getActiveJobId()

            memory.addPayout(targetJobId, accountId, net)

            println(EngineAnsi.GREEN + "      💾 [Spring_Memory] Saved $net for Account $accountId (Target: $targetJobId)" + EngineAnsi.RESET)
        }

        // Trace Log
        val percentage = taxRate.multiply(BigDecimal(100)).toInt()
        println("      " + EngineAnsi.YELLOW + "💸 $prefix Tax ($percentage%): -$tax | Net: $net THB" + EngineAnsi.RESET)
    }

    override fun initialize(context: KernelContext) { memory.clearJob(context.jobId) }
    override fun shutdown() {}
}