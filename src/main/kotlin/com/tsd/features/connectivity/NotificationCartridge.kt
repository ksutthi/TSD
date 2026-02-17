package com.tsd.features.connectivity

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class NotificationCartridge : Cartridge {
    override val id: String = "Notification"
    override val version: String = "1.0"
    override val priority: Int = 130

    override fun initialize(context: ExecutionContext) {}
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.CYAN}[$id] 🔔 Sending email/SMS alerts...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}