package com.tsd.features.distribution

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component("Execute_Swift_Payment")
class ExecuteSwiftPaymentCartridge : Cartridge {
    override val id = "Execute_Swift_Payment"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val amount = packet.data["Net_Amount"]
        val currency = packet.data["Country_Code"] ?: "THB"
        println("      ✈️ [N3] Sending SWIFT MT103 ($currency): SENT $amount")
    }
    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}