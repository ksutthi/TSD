package com.tsd.app.functional_area.eligibility.cartridge

import com.tsd.app.functional_area.account.repo.AccountBalanceRepository
import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.math.BigDecimal
import com.tsd.platform.engine.util.EngineAnsi

@Component("Ingest_CSD_Scripless")
class IngestCSDScriplessCartridge(
    private val accountRepo: AccountBalanceRepository
) : Cartridge {

    // 🟢 Standard Boilerplate
    override val id: String = "Ingest_CSD_Scripless"
    override val version: String = "1.0"
    override val priority: Int = 10

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val accountId = packet.data["Account_ID"]?.toString()?.toLongOrNull() ?: return

        // 🟢 1. Get Dynamic Prefix
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[??]"

        print(EngineAnsi.GRAY + "   📥 $prefix Reading Balance from Local Ledger..." + EngineAnsi.RESET)
        println("")

        // 🟢 READ DIRECTLY FROM TABLE
        val account = accountRepo.findById(accountId).orElse(null)

        if (account != null) {
            val balance = account.quantity // Read the actual DB value

            // 2. UPDATE PACKET
            packet.data["Share_Balance"] = balance

            // 🟢 ALIGNMENT FIX: 6 spaces outer + 6 spaces inner
            println("      " + EngineAnsi.GREEN + "    ✅ $prefix Balance Loaded: $balance shares" + EngineAnsi.RESET)
        } else {
            // 🟢 ALIGNMENT FIX: 6 spaces outer + 6 spaces inner
            println("      " + EngineAnsi.RED + "      ⚠️ $prefix Account $accountId not found in Local Ledger!" + EngineAnsi.RESET)
            packet.data["Share_Balance"] = java.math.BigDecimal.ZERO
        }
    }
}