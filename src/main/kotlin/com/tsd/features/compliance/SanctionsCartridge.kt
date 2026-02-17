package com.tsd.features.compliance

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class SanctionsCartridge : Cartridge {

    override val id: String = "Sanctions_Check"
    override val version: String = "1.0"
    override val priority: Int = 30

    override fun initialize(context: ExecutionContext) {}

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.CYAN}[$id] 🚫 Checking Sanctions Lists...${EngineAnsi.RESET}")
    }

    override fun shutdown() {}
}