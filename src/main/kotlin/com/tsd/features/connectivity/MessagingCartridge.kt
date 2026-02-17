package com.tsd.features.connectivity

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class MessagingCartridge : Cartridge {
    override val id: String = "Messaging_Service"
    override val version: String = "1.0"
    override val priority: Int = 120

    override fun initialize(context: ExecutionContext) {}
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📨 Sending SWIFT/ISO20022 messages...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}