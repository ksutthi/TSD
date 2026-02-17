package com.tsd.features.account

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext // ✅ Correct Import
import org.springframework.stereotype.Component

@Component
class RegistryDataCartridge : Cartridge {
    override val id: String = "RegistryData"
    override val version: String = "1.0"
    override val priority: Int = 10

    // 🟢 CHANGED: KernelContext -> ExecutionContext
    override fun initialize(context: ExecutionContext) {}

    // 🟢 CHANGED: KernelContext -> ExecutionContext
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val prefix = "[SEQ:$priority] [SRC:${this::class.simpleName}]"

        println("   ${EngineAnsi.CYAN}$prefix 📂 STARTED: Loading Account Data...${EngineAnsi.RESET}")

        // --- SIMULATE DATABASE FETCH ---
        val fetchedAmount = "10000.00"
        val fetchedRate = "1.25"

        println("      ${EngineAnsi.YELLOW}$prefix ➕ Injecting: GROSS_PAYOUT = $fetchedAmount${EngineAnsi.RESET}")
        packet.data["GROSS_PAYOUT"] = fetchedAmount

        println("      ${EngineAnsi.YELLOW}$prefix ➕ Injecting: FX_RATE      = $fetchedRate${EngineAnsi.RESET}")
        packet.data["FX_RATE"] = fetchedRate

        println("      ${EngineAnsi.GREEN}$prefix ✅ COMPLETED.${EngineAnsi.RESET}")
    }

    override fun shutdown() {}
}