package com.tsd.app.reporting.cartridge

import com.tsd.core.service.AuditLedger
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component

@Component("Reporting_Engine")
class ReportingCartridge(
    private val auditor: AuditLedger
) : Cartridge {

    override val id = "Reporting_Engine"
    override val version = "1.0"
    override val priority = 140 // Runs Last

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // 🟢 PRINT THE FINAL SCORECARD
        println("\n" + EngineAnsi.BLUE + auditor.getReport() + EngineAnsi.RESET)
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}