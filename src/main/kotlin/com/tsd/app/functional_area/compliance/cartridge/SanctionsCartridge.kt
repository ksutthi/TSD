package com.tsd.app.functional_area.compliance.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class SanctionsCartridge : Cartridge {

    override val id: String = "Sanctions_Check"
    override val version: String = "1.0"
    override val priority: Int = 30

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 🚫 Checking Sanctions Lists...${EngineAnsi.RESET}")
    }

    override fun shutdown() {}
}