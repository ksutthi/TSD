package com.tsd.app.distribution.cartridge

import com.tsd.app.market.cartridge.FxRateCartridge
import com.tsd.app.calculation.cartridge.FeeCalculatorCartridge
import com.tsd.app.audit.service.AuditLedger
import com.tsd.platform.engine.state.JobAccumulator // 🟢 IMPORT BEAN
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.event.PaymentCompletedEvent
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

@Component("Gen_50Tawi_PDF")
class Gen50TawiPdfCartridge(
    private val publisher: ApplicationEventPublisher,
    private val fxRacer: FxRateCartridge,
    private val feeShopper: FeeCalculatorCartridge,
    private val auditor: AuditLedger,
    private val memory: JobAccumulator // 🟢 INJECT BEAN
) : Cartridge {

    override val id = "Gen_50Tawi_PDF"
    override val version = "5.0" // Spring Bean Version
    override val priority = 50

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = "[N3-SPRING]"

        // 1. Strategies (Race & Scatter-Gather)
        println(EngineAnsi.CYAN + "      🏁 $prefix [RACE] Getting FX Rate for Batch..." + EngineAnsi.RESET)
        fxRacer.execute(packet, context)

        println(EngineAnsi.CYAN + "      🛍️ $prefix [SCATTER-GATHER] Calculating Fees for Batch..." + EngineAnsi.RESET)
        feeShopper.execute(packet, context)

        // 2. Fetch from Spring Memory
        println(EngineAnsi.CYAN + "      🏗️ $prefix Starting Bulk PDF Generation from Spring Memory..." + EngineAnsi.RESET)

        // 🟢 READ (Pass Job ID - The Accumulator handles the Session fallback automatically)
        val allPayments = memory.getAllPayouts(context.jobId)

        if (allPayments.isEmpty()) {
            println(EngineAnsi.YELLOW + "      ⚠️ No pending payments found in Spring Memory." + EngineAnsi.RESET)
            return
        }

        println(EngineAnsi.CYAN + "      📚 Found ${allPayments.size} pending payments." + EngineAnsi.RESET)

        // 3. Process Loop
        allPayments.forEach { (accountId, amount) ->
            processSingleAccount(prefix, accountId, amount, context)
        }

        println(EngineAnsi.GREEN + "      ✅ $prefix Bulk Processing Completed." + EngineAnsi.RESET)
    }

    private fun processSingleAccount(prefix: String, accountId: Long, amount: BigDecimal, context: KernelContext) {
        auditor.recordAttempt(amount)

        try {
            // Generate PDF (Simulated)
            val filename = "50Tawi_${accountId}_${UUID.randomUUID().toString().substring(0,8)}.pdf"
            println("         📄 Generated $filename for Account $accountId ($amount THB)")

            // Broadcast Event
            publisher.publishEvent(PaymentCompletedEvent(this, accountId, amount))

            // 🟢 SUCCESS: Remove from Spring Memory (Pass Job ID)
            memory.removePayout(context.jobId, accountId)

            auditor.recordSuccess(amount)

        } catch (e: Exception) {
            println(EngineAnsi.RED + "         ❌ $prefix Failed to process Account $accountId" + EngineAnsi.RESET)
            auditor.recordFailure()
            // We do NOT remove it from memory, allowing for retry (it stays pending)
        }
    }

    override fun initialize(context: KernelContext) {
        auditor.reset()
    }
    override fun shutdown() {}
}