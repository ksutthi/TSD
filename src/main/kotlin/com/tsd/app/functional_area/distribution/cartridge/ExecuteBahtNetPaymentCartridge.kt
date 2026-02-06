package com.tsd.app.functional_area.distribution.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component("Execute_BahtNet_Payment")
class ExecuteBahtNetPaymentCartridge : Cartridge {
    override val id = "Execute_BahtNet_Payment"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val amount = packet.data["Net_Amount"]
        println("      🏦 [N3] Sending BahtNet Instruction: SENT $amount")
    }
    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}