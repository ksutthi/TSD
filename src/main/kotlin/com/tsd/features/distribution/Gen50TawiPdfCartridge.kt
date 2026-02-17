package com.tsd.features.distribution

import com.tsd.features.market.FxRateCartridge
import com.tsd.features.calculation.FeeCalculatorCartridge
import com.tsd.core.service.AuditLedger
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.core.event.PaymentCompletedEvent
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
// 🟢 IMPORT IMPLEMENTATION (For advanced Payout helpers)
import com.tsd.platform.engine.state.KernelContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

@Component("Gen_50Tawi_PDF")
class Gen50TawiPdfCartridge(
    private val publisher: ApplicationEventPublisher,
    private val fxRacer: FxRateCartridge,
    private val feeShopper: FeeCalculatorCartridge,
    private val auditor: AuditLedger
    // 🟢 REMOVED: JobAccumulator (We use the Engine's KernelContext memory now)
) : Cartridge {

    override val id = "Gen_50Tawi_PDF"
    override val version = "5.0"
    override val priority = 50

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val prefix = "[N3-SPRING]"

        // 1. Strategies (Race & Scatter-Gather)
        println(EngineAnsi.CYAN + "      🏁 $prefix [RACE] Getting FX Rate for Batch..." + EngineAnsi.RESET)
        fxRacer.execute(packet, context)

        println(EngineAnsi.CYAN + "      🛍️ $prefix [SCATTER-GATHER] Calculating Fees for Batch..." + EngineAnsi.RESET)
        feeShopper.execute(packet, context)

        // 2. Fetch from Engine Memory
        println(EngineAnsi.CYAN + "      🏗️ $prefix Starting Bulk PDF Generation from Engine Memory..." + EngineAnsi.RESET)

        // 🟢 READ: Use KernelContext helpers directly (Safe Cast)
        val kernel = context as? KernelContext
        val allPayments = kernel?.getAllJobPayouts() ?: emptyMap()

        if (allPayments.isEmpty()) {
            println(EngineAnsi.YELLOW + "      ⚠️ No pending payments found in Engine Memory." + EngineAnsi.RESET)
            return
        }

        println(EngineAnsi.CYAN + "      📚 Found ${allPayments.size} pending payments." + EngineAnsi.RESET)

        // 3. Process Loop
        allPayments.forEach { (accountId, amount) ->
            processSingleAccount(prefix, accountId, amount, context)
        }

        println(EngineAnsi.GREEN + "      ✅ $prefix Bulk Processing Completed." + EngineAnsi.RESET)
    }

    // 🟢 FIXED SIGNATURE: context is now ExecutionContext
    private fun processSingleAccount(prefix: String, accountId: Long, amount: BigDecimal, context: ExecutionContext) {
        auditor.recordAttempt(amount)

        try {
            // Generate PDF (Simulated)
            val filename = "50Tawi_${accountId}_${UUID.randomUUID().toString().substring(0,8)}.pdf"
            println("         📄 Generated $filename for Account $accountId ($amount THB)")

            // Broadcast Event
            publisher.publishEvent(PaymentCompletedEvent(this, accountId, amount))

            // 🟢 SUCCESS: Remove from Memory
            // We use the helper if available
            (context as? KernelContext)?.removePayoutFromJob(accountId)

            auditor.recordSuccess(amount)

        } catch (e: Exception) {
            println(EngineAnsi.RED + "         ❌ $prefix Failed to process Account $accountId" + EngineAnsi.RESET)
            auditor.recordFailure()
            // We do NOT remove it from memory, allowing for retry (it stays pending)
        }
    }

    override fun initialize(context: ExecutionContext) {
        auditor.reset()
    }

    override fun shutdown() {}
}