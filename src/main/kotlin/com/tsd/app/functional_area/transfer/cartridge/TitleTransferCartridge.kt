package com.tsd.app.functional_area.transfer.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class TitleTransferCartridge : Cartridge {
    override val id: String = "Title_Transfer"
    override val version: String = "1.0"
    override val priority: Int = 110

    override fun initialize(context: KernelContext) {}
    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📜 Transferring legal ownership...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}