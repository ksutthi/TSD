package com.tsd.app.functional_area.compliance.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class AuditCartridge : Cartridge {

    override val id: String = "Audit_Log"
    override val version: String = "1.0"
    override val priority: Int = 100 // Runs late in the pipeline to capture everything

    override fun initialize(context: KernelContext) {
        // Init logic
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   ${EngineAnsi.CYAN}[$id] 📝 Auditing transaction...${EngineAnsi.RESET}")
    }

    override fun shutdown() {
        // Cleanup logic
    }
}