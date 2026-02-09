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
        packet.data["WHT_Rate"] = BigDecimal("0.10")
        println("      🏛️ [M1] Tax Tables Loaded: Rate = 10%")
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}