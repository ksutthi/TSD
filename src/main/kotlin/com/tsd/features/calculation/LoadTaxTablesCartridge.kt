package com.tsd.features.calculation

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class LoadTaxTablesCartridge : Cartridge {
    override val id = "Load_Tax_Tables"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val rate = BigDecimal("0.10")

        // 🟢 FIX 1: Save to CONTEXT (So Tax Engine can read it)
        context.set("WHT_Rate", rate)

        // 🟢 FIX 2: Save to Packet (Legacy backup)
        packet.data["WHT_Rate"] = rate

        println("      🏛️ [M1] Tax Tables Loaded: Rate = 10%")
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}