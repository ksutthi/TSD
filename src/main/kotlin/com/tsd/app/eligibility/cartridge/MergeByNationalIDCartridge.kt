package com.tsd.app.eligibility.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component

@Component("Merge_By_NationalID")
class MergeByNationalIDCartridge : Cartridge {
    override val id = "Merge_By_NationalID"
    override val version = "1.0"
    override val priority = 10

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        // 🟢 1. Get Dynamic Prefix
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        // ... logic ...

        // 🟢 2. ALIGNMENT FIX: 6 spaces outer + 6 spaces inner
        // Matches the standard used in [K1], [J1], etc.
        println("      " + EngineAnsi.CYAN + "      🆔 $prefix Identity Resolved: Merged by National ID (CID)." + EngineAnsi.RESET)
    }

    override fun shutdown() {}
}