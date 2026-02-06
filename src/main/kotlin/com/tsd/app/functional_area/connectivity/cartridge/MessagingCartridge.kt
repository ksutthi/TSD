package com.tsd.app.functional_area.connectivity.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class MessagingCartridge : Cartridge {
    override val id: String = "Messaging_Service"
    override val version: String = "1.0"
    override val priority: Int = 120

    override fun initialize(context: KernelContext) {}
    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📨 Sending SWIFT/ISO20022 messages...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}