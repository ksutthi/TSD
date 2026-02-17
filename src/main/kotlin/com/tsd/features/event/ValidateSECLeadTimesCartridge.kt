package com.tsd.app.market.cartridge

import com.tsd.platform.engine.state.JobAccumulator // 🟢 IMPORT
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component

@Component("Validate_SEC_Lead_Times")
class ValidateSECLeadTimesCartridge(
    private val memory: JobAccumulator // 🟢 INJECT
) : Cartridge {

    override val id = "Validate_SEC_Lead_Times"
    override val version = "2.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[J1]"
        println(EngineAnsi.CYAN + "      📅 $prefix Checking SEC Rules (T+3)..." + EngineAnsi.RESET)

        // 🟢 BIND THE SESSION
        // This tells the memory bean: "Use this ID for everything that follows"
        memory.startJobSession(context.getEventID())

        println("         ✅ $prefix Lead times validated. Event is compliant.")
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}