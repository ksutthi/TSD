package com.tsd.app.settlement.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.util.SecretContext
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component("Gen_Media_Clearing_File")
class GenMediaClearingFileCartridge : Cartridge {

    override val id = "Gen_Media_Clearing_File"
    override val version = "2.0" // 🟢 Upgraded to Bulk Aggregator
    override val priority = 30   // Runs BEFORE PDF Generation (Prio 50)

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = "[N2-BULK]"
        println(EngineAnsi.CYAN + "      🏦 $prefix Generating Master Bank Media Clearing File..." + EngineAnsi.RESET)

        // 1. Fetch ALL Data (Read-Only)
        // We do NOT use 'withdraw' here because the PDF Generator (Priority 50)
        // needs to find the money in the vault later.
        val allPayments = SecretContext.findAll()

        if (allPayments.isEmpty()) {
            println(EngineAnsi.YELLOW + "      ⚠️ No data to report to Bank." + EngineAnsi.RESET)
            return
        }

        // 2. Aggregate Data
        var totalAmount = BigDecimal.ZERO
        var recordCount = 0
        val fileContent = StringBuilder()

        // --- Header Record ---
        fileContent.append("H|TSD|${LocalDate.now()}|BATCH001\n")

        // --- Body Records ---
        allPayments.forEach { (accountId, amount) ->
            totalAmount = totalAmount.add(amount)
            recordCount++
            fileContent.append("D|$accountId|$amount|THB|SMART_CREDIT\n")
        }

        // --- Footer Record ---
        fileContent.append("T|$recordCount|$totalAmount\n")

        // 3. "Save" File (Simulation)
        println(EngineAnsi.GREEN + "      ✅ $prefix File 'MEDIA_CLEARING_2026.TXT' generated successfully." + EngineAnsi.RESET)
        println(EngineAnsi.GREEN + "      💰 $prefix Total Settlement Value: $totalAmount THB ($recordCount records)" + EngineAnsi.RESET)

        // 4. Store Summary for Reporting Engine (runs at Z1)
        packet.data["Bank_Total"] = totalAmount
        packet.data["Bank_Record_Count"] = recordCount
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}