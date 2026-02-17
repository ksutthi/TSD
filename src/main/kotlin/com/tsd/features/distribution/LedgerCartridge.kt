package com.tsd.features.distribution

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class LedgerCartridge : Cartridge {
    override val id: String = "Ledger_Booking"
    override val version: String = "1.0"
    override val priority: Int = 90

    override fun initialize(context: ExecutionContext) {}
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📒 Booking transaction to Ledger...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}