package com.tsd.app.connectivity.cartridge

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class ReportingCartridge : Cartridge {
    override val id: String = "Reporting_Engine"
    override val version: String = "1.0"
    override val priority: Int = 140

    override fun initialize(context: KernelContext) {}
    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📊 Generating daily transaction report...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}