package com.tsd.app.servicing.cartridge

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class CorporateEventCartridge : Cartridge {
    override val id: String = "Corp_Action_Check"
    override val version: String = "1.0"
    override val priority: Int = 80

    override fun initialize(context: KernelContext) {}
    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📢 Checking for corporate actions...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}