package com.tsd.app.calculation.cartridge

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class LoadTaxTablesCartridge : Cartridge {
    override val id = "Load_Tax_Tables"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val rate = BigDecimal("0.10")

        // 🟢 FIX 1: Save to CONTEXT (So Tax Engine can read it)
        context.set("WHT_Rate", rate)

        // 🟢 FIX 2: Save to Packet (Legacy backup)
        packet.data["WHT_Rate"] = rate

        println("      🏛️ [M1] Tax Tables Loaded: Rate = 10%")
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}