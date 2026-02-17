package com.tsd.features.stub

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component

@Component
class FunctionalStubCartridge : Cartridge {
    override val id: String = "Functional_Debug_Stub"
    override val version: String = "0.0.1"
    override val priority: Int = 999

    override fun initialize(context: ExecutionContext) {}
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.GRAY}[$id] (Stub executed)${EngineAnsi.RESET}")
    }
    override fun shutdown() {}
}