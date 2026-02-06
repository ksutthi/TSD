package com.tsd.app.functional_area.connectivity.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class NotificationCartridge : Cartridge {
    override val id: String = "Notification"
    override val version: String = "1.0"
    override val priority: Int = 130

    override fun initialize(context: KernelContext) {}
    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 🔔 Sending email/SMS alerts...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}