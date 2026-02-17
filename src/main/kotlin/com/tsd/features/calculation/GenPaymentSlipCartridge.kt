package com.tsd.features.calculation

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class GenPaymentSlipCartridge : Cartridge {
    override val id = "Gen_Payment_Slip"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("      📄 [M6] Generating Payment Slips: DONE")
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}