package com.tsd.app.market.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Enrich_Market_Data") // 🟢 CRITICAL: This matches 'Enrich_Market_Data' in workflow_matrix.csv
class GetMarketPriceCartridge : Cartridge {

    override val id = "Enrich_Market_Data"
    override val version = "1.0"
    override val priority = 10

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[MARKET]"

        println(EngineAnsi.CYAN + "      📈 $prefix Fetching Closing Price from SET..." + EngineAnsi.RESET)

        // 1. Simulate fetching price (e.g. PTT = 34.50)
        // In a real app, this would call an external Market Data Feed API
        val symbol = packet.data["Security_Symbol"] ?: "UNKNOWN"
        val price = BigDecimal("34.50") // Mocked Price

        // 2. Enrich the packet
        packet.data["Market_Price"] = price

        // 3. Log
        println("         ✅ Price found for $symbol: $price THB")
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}