package com.tsd.app.distribution.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.util.SecretContext
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Auto_Pass_Standard")
class AutoPassStandardCartridge : Cartridge {

    override val id = "Auto_Pass_Standard"
    override val version = "2.0" // 🟢 Upgraded to Bulk Version
    override val priority = 10

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = "[N1-BULK]"
        val threshold = BigDecimal("1000000.00")

        // 1. 🟢 THE FIX: Look in the Vault, not the empty Context
        val allAccounts = SecretContext.findAll()

        if (allAccounts.isEmpty()) {
            println("         ⚠️ $prefix No accounts to check.")
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