package com.tsd.features.transfer

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component

@Component
class TitleTransferCartridge : Cartridge {
    override val id: String = "Title_Transfer"
    override val version: String = "1.0"
    override val priority: Int = 110

    override fun initialize(context: ExecutionContext) {}
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📜 Transferring legal ownership...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}