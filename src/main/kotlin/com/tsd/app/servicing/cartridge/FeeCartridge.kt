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
class FeeCartridge : Cartridge {
    override val id: String = "Fee_Calculator"
    override val version: String = "1.0"
    override val priority: Int = 70

    private var baseFee: BigDecimal = BigDecimal("5.00")

    override fun initialize(context: KernelContext) {
        loadFeeStructure()
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 💰 Applying Transaction Fee: $baseFee THB${EngineAnsi.RESET}")
        context.set("FEE_AMOUNT", baseFee)
    }

    private fun loadFeeStructure() {
        val filePath = "/config/cartridges/99_business_params.csv"
        try {
            val inputStream = javaClass.getResourceAsStream(filePath)
            if (inputStream != null) {
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readLine() // Skip Header

                    reader.forEachLine { line ->
                        // CSV Structure: TYPE, ID, KEY, SUB_KEY, VALUE
                        val parts = line.split(",").map { it.trim() }

                        // We look for ID="FEE" and SUB_KEY="FEE_PER_TXN"
                        if (parts.size >= 5 && parts[1] == "FEE" && parts[3] == "FEE_PER_TXN") {
                            baseFee = BigDecimal(parts[4])
                            println("      📋 [Fee_Calculator] Loaded Config: Base Fee = $baseFee")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("      ❌ [Fee_Calculator] Error loading config: ${e.message}")
        }
    }

    override fun shutdown() {}
}