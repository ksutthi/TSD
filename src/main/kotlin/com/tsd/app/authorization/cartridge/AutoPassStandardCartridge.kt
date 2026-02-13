package com.tsd.app.distribution.cartridge

import com.tsd.platform.engine.state.JobAccumulator // 🟢 USE BEAN
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Auto_Pass_Standard")
class AutoPassStandardCartridge(
    private val memory: JobAccumulator // 🟢 INJECT BEAN
) : Cartridge {

    override val id = "Auto_Pass_Standard"
    override val version = "4.0" // Production Version
    override val priority = 10

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = "[N1-SPRING]" // Updated tag to match new architecture
        val threshold = BigDecimal("1000000.00")

        // 🟢 1. THE FIX: Look in the Spring Bean Vault using the Session ID
        val allAccounts = memory.getAllPayouts(context.jobId)

        if (allAccounts.isEmpty()) {
            println(EngineAnsi.YELLOW + "      ⚠️ $prefix No accounts to check." + EngineAnsi.RESET)
            return
        }

        // 2. Count real standard transactions
        val standardCount = allAccounts.count { it.value <= threshold }
        val totalCount = allAccounts.size

        println(EngineAnsi.CYAN + "      🛡️ $prefix Analyzing Population..." + EngineAnsi.RESET)
        println(EngineAnsi.CYAN + "         📉 Standard (< 1M): $standardCount / $totalCount" + EngineAnsi.RESET)

        if (standardCount > 0) {
            println(EngineAnsi.GREEN + "      ✅ $prefix Auto-Authorized $standardCount standard transactions." + EngineAnsi.RESET)
        }
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}