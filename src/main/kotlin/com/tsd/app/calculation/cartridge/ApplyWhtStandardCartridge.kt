package com.tsd.app.calculation.cartridge

import com.tsd.app.calculation.service.DividendCalculation
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.math.BigDecimal
import com.tsd.platform.engine.util.SecretContext

@Component("Apply_WHT_Standard")
class ApplyWhtStandardCartridge(
    private val dividendCalculation: DividendCalculation
) : Cartridge {
    override val id = "Apply_WHT_Standard"
    override val version = "1.0"
    override val priority = 4

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        // 1. Get Data (Modern Way: from Context)
        val gross = context.getAmount("Gross_Amount")
        val taxRateRaw = context.getAmount("WHT_Rate")
        // 🕵️‍♂️ DEBUG LOGGING
        println("      " + EngineAnsi.CYAN + "🕵️‍♂️ $prefix [DEBUG] Reading from context:" + EngineAnsi.RESET)
        println("      " + EngineAnsi.CYAN + "   - Gross_Amount: $gross" + EngineAnsi.RESET)
        println("      " + EngineAnsi.CYAN + "   - WHT_Rate: $taxRateRaw" + EngineAnsi.RESET)

        // Default tax to 10% if not found
        val taxRate = if (taxRateRaw > BigDecimal.ZERO) taxRateRaw else BigDecimal("0.10")

        // 2. Use Service for Math
        val tax = dividendCalculation.calculateTax(gross, taxRate)
        val net = dividendCalculation.calculateNet(gross, tax)

        // 3. Save Result (to both Context and Packet)
        context.set("Tax_Amount", tax)
        context.set("Net_Amount", net)
        packet.data["Tax_Amount"] = tax
        packet.data["Net_Amount"] = net

        // 3. Save Result
        context.set("Net_Amount", net)
        packet.data["Net_Amount"] = net

                // 🟢 TELEPORT: Deposit money into the Secret Vault
        val accountIdStr = context.getString("Account_ID")
        val accountId = accountIdStr.toLongOrNull()
        if (accountId != null) {
            SecretContext.deposit(accountId, net)
            println(EngineAnsi.GREEN + "      🔒 [Secret_Vault] Deposited $net for Account $accountId" + EngineAnsi.RESET)
        }


        // 🕵️ TRACE LOG: Did it actually stick?
        val keys = packet.data.keys.joinToString(", ")
        println(EngineAnsi.YELLOW + "      📦 [Tax_Engine] Packet Keys AFTER Write: [$keys]" + EngineAnsi.RESET)

        // Double check immediate read
        val check = packet.data["Net_Amount"]
        println(EngineAnsi.YELLOW + "      👀 [Tax_Engine] Immediate Read Check: $check" + EngineAnsi.RESET)

        val percentage = taxRate.multiply(BigDecimal(100)).toInt()
        println("      " + EngineAnsi.YELLOW + "💸 $prefix Tax ($percentage%): -$tax | Net: $net THB" + EngineAnsi.RESET)
        println("      Within ApplyWhtStandardCartridge")
        println("      Check the Net_Amount")
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}