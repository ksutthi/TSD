package com.tsd.app.functional_area.account.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component
class RegistryDataCartridge : Cartridge {
    override val id: String = "RegistryData"
    override val version: String = "1.0"

    // 1️⃣ RUNNING NUMBER (Sequence)
    // 10 means "I run early, in the Input Phase".
    override val priority: Int = 10

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 2️⃣ "WHERE I CAME FROM" (Traceability)
        // We create a standardized prefix: [SEQ: ##] [SRC: ClassName]
        val prefix = "[SEQ:$priority] [SRC:${this::class.simpleName}]"

        println("   ${EngineAnsi.CYAN}$prefix 📂 STARTED: Loading Account Data...${EngineAnsi.RESET}")

        // --- SIMULATE DATABASE FETCH ---
        val fetchedAmount = "10000.00"
        val fetchedRate = "1.25"

        // 3️⃣ LOGGING THE ACTIONS
        println("      ${EngineAnsi.YELLOW}$prefix ➕ Injecting: GROSS_PAYOUT = $fetchedAmount${EngineAnsi.RESET}")
        packet.data["GROSS_PAYOUT"] = fetchedAmount

        println("      ${EngineAnsi.YELLOW}$prefix ➕ Injecting: FX_RATE      = $fetchedRate${EngineAnsi.RESET}")
        packet.data["FX_RATE"] = fetchedRate

        println("      ${EngineAnsi.GREEN}$prefix ✅ COMPLETED.${EngineAnsi.RESET}")
    }

    override fun shutdown() {}
}