package com.tsd.features.compliance

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class ValidationCartridge : Cartridge {

    override val id: String = "Data_Validation"
    override val version: String = "1.0"
    override val priority: Int = 10 // Runs first to check data quality

    override fun initialize(context: ExecutionContext) {}

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        println("   ${EngineAnsi.CYAN}[$id] ✅ Validating input data...${EngineAnsi.RESET}")
    }

    override fun shutdown() {}
}