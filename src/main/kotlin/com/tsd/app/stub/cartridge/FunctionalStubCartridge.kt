package com.tsd.app.stub.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class FunctionalStubCartridge : Cartridge {
    override val id: String = "Functional_Debug_Stub"
    override val version: String = "0.0.1"
    override val priority: Int = 999

    override fun initialize(context: KernelContext) {}
    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.GRAY}[$id] (Stub executed)${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}