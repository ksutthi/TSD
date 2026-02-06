package com.tsd.app.functional_area.market.catridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class FxRateCartridge : Cartridge {
    override val id: String = "Fx_Market_Rates"
    override val version: String = "1.0"
    override val priority: Int = 50

    override fun initialize(context: KernelContext) {}
    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 💱 Fetching latest FX rates...${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}