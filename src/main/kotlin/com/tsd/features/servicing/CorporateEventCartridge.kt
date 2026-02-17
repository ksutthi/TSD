package com.tsd.features.servicing

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component
import com.tsd.platform.engine.state.KernelContext

@Component
class CorporateEventCartridge : Cartridge {
    override val id: String = "Corp_Action_Check"
    override val version: String = "1.0"
    override val priority: Int = 80

    override fun initialize(context: ExecutionContext) {}
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📢 Checking for corporate actions...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}