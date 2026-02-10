package com.tsd.app.settlement.cartridge // (Check your package)

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.util.SecretContext // 🟢 IMPORT THIS
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Gen_Media_Clearing_File")
class GenMediaClearingFileCartridge : Cartridge {
    override val id = "Gen_Media_Clearing_File"
    override val version = "1.0"
    override val priority = 30

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[N2]"

        // 1. Try Context/Packet
        var amount = context.getAmount("Net_Amount")

        // 2. 🟢 TELEPORT RECOVERY: Check the Vault!
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            val accountIdStr = context.getString("Account_ID")
            val accountId = accountIdStr?.toLongOrNull()

            if (accountId != null) {
                val secretMoney = SecretContext.withdraw(accountId)
                if (secretMoney != null) {
                    amount = secretMoney
                }
            }
        }

        // 3. Generate File
        println(EngineAnsi.CYAN + "      🏦 $prefix Generating Bank Media Clearing File for: $amount THB" + EngineAnsi.RESET)

        // Simulate file creation
        println(EngineAnsi.GREEN + "      ✅ $prefix Media File: MEDIA_CLEARING_20260202.TXT created." + EngineAnsi.RESET)
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}