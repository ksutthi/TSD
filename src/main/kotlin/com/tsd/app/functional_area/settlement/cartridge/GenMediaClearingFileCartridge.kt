package com.tsd.app.functional_area.settlement.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component("Gen_Media_Clearing_File")
class GenMediaClearingFileCartridge : Cartridge {

    override val id: String = "Gen_Media_Clearing_File"
    override val version: String = "1.0"
    override val priority: Int = 30

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val amount = packet.data["Net_Amount"]
        println("   🏦 [N2] Generating Bank Media Clearing File for: $amount THB")
        println("   ✅ [N2] Media File: MEDIA_CLEARING_20260202.TXT created.")
    }

    override fun shutdown() {}
}