package com.tsd.features.eligibility

import com.tsd.adapter.output.persistence.AccountBalanceRepository
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import com.tsd.platform.engine.util.EngineAnsi
import java.math.BigDecimal

@Component("Ingest_CSD_Scripless")
class IngestCSDScriplessCartridge(
    private val accountRepo: AccountBalanceRepository
) : Cartridge {

    override val id: String = "Ingest_CSD_Scripless"
    override val version: String = "1.1" // Bumped Version
    override val priority: Int = 10

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // 🟢 FIX: Extract Account ID from the Packet ID string (e.g., "EVT-5-XXX")
        // Split "EVT-5-AB12" by "-" -> ["EVT", "5", "AB12"] -> Get item [1]
        val parts = packet.id.split("-")
        val accountId = if (parts.size >= 2) parts[1].toLongOrNull() else null

        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[INGEST]"

        if (accountId == null) {
            println(EngineAnsi.RED + "      ❌ $prefix Critical Error: Could not parse Account ID from '${packet.id}'" + EngineAnsi.RESET)
            return
        }

        println(EngineAnsi.GRAY + "      📥 $prefix Reading Balance for Acct $accountId (Parsed from ID)..." + EngineAnsi.RESET)

        // 🟢 READ DIRECTLY FROM VAULT
        val accountOpt = accountRepo.findById(accountId)

        if (accountOpt.isPresent) {
            val account = accountOpt.get()
            val balance = account.quantity

            // 2. UPDATE PACKET
            packet.data["Share_Balance"] = balance
            packet.data["Participant_ID"] = account.participantId
            packet.data["Tax_Profile"] = account.taxProfile
            packet.data["Country_Code"] = account.countryCode
            packet.data["Account_ID"] = accountId // Store it back for others to use safely

            println(EngineAnsi.GREEN + "         ✅ $prefix Balance Loaded: $balance shares" + EngineAnsi.RESET)
        } else {
            println(EngineAnsi.RED + "         ⚠️ $prefix Account $accountId not found in Vault!" + EngineAnsi.RESET)
            packet.data["Share_Balance"] = BigDecimal.ZERO
        }
    }
}