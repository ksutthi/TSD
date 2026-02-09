package com.tsd.platform.cartridges.business

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal

@Component
class TaxCartridge : Cartridge {
    override val id: String = "Tax_Engine"
    override val version: String = "1.0"
    override val priority: Int = 75

    // Default to 10%
    private var taxRate: BigDecimal = BigDecimal("0.10")

    override fun initialize(context: KernelContext) {
        loadTaxRules()
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 1. Calculate Tax (Assuming "GROSS_AMOUNT" exists)
        // Use safer parsing if the value might be empty
        val grossStr = context.getString("GROSS_AMOUNT")
        val gross = if (grossStr.isNotBlank()) BigDecimal(grossStr) else BigDecimal.ZERO

        val taxAmount = gross.multiply(taxRate)
        val net = gross.subtract(taxAmount)

        // 2. Save to Context
        context.set("TAX_AMOUNT", taxAmount)
        context.set("NET_AMOUNT", net)

        println("   ${EngineAnsi.CYAN}[$id] 🧾 Tax Applied (${taxRate.multiply(BigDecimal(100)).toInt()}%): -$taxAmount | Net: $net${EngineAnsi.RESET}")
    }

    private fun loadTaxRules() {
        // 🟢 Point to the correct file location
        val filePath = "/config/cartridges/99_business_params.csv"
        try {
            val inputStream = javaClass.getResourceAsStream(filePath)
            if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip Header
                    reader.readLine()

                    reader.forEachLine { line ->
                        // CSV Structure: TYPE, ID, KEY, SUB_KEY, VALUE
                        val parts = line.split(",").map { it.trim() }

                        // We look for ID="TAX" and SUB_KEY="TAX_RATE_IND_LOCAL" (Col 2 and 4)
                        if (parts.size >= 5 && parts[1] == "TAX" && parts[3] == "TAX_RATE_IND_LOCAL") {
                            taxRate = BigDecimal(parts[4])
                            println("      📋 [Tax_Engine] Loaded Config: Rate = $taxRate")
                        }
                    }
                }
            } else {
                println("      ⚠️ [Tax_Engine] Config file not found: $filePath (Using default 0.10)")
            }
        } catch (e: Exception) {
            println("      ❌ [Tax_Engine] Error loading config: ${e.message}")
        }
    }

    override fun shutdown() {}
}