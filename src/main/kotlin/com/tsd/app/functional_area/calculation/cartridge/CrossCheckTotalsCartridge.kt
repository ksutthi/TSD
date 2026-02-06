package com.tsd.app.functional_area.calculation.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class CrossCheckTotalsCartridge : Cartridge {
    override val id = "Cross_Check_Totals"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // Validation logic will go here later
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}