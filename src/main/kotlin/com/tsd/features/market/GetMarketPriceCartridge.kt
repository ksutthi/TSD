package com.tsd.features.market

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Enrich_Market_Data")
class GetMarketPriceCartridge : Cartridge {

    override val id = "Enrich_Market_Data"
    override val version = "1.0"
    override val priority = 10

    override fun initialize(context: ExecutionContext) {
        // Initialization logic if needed (e.g. connect to Market Data Feed)
    }

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[MARKET]"

        println(EngineAnsi.CYAN + "      📈 $prefix Fetching Closing Price from SET..." + EngineAnsi.RESET)

        // 1. Simulate fetching price (e.g. PTT = 34.50)
        val symbol = packet.data["Security_Symbol"] ?: "UNKNOWN"
        val price = BigDecimal("34.50") // Mocked Price

        // 2. Enrich the packet
        packet.data["Market_Price"] = price

        // 3. Log
        println("         ✅ Price found for $symbol: $price THB")
    }

    // 🟢 Added compensate to satisfy the Interface contract (Safety)
    override fun compensate(packet: ExchangePacket, context: ExecutionContext) {
        // No-op: Reading a price is a read-only operation, nothing to rollback.
    }

    override fun shutdown() {
        // Cleanup logic if needed
    }
}