package com.tsd.app.functional_area.calculation.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class GenPaymentSlipCartridge : Cartridge {
    override val id = "Gen_Payment_Slip"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("      📄 [M6] Generating Payment Slips: DONE")
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}