package com.tsd.app.distribution.cartridge

import com.tsd.app.market.cartridge.FxRateCartridge
import com.tsd.app.calculation.cartridge.FeeCalculatorCartridge
import com.tsd.app.audit.service.AuditLedger // 🟢 IMPORT THE AUDITOR
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.engine.util.SecretContext
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
    private val auditor: AuditLedger // 🟢 1. INJECT AUDITOR
) : Cartridge {

    override val id = "Gen_50Tawi_PDF"
    override val version = "3.0" // 🟢 UPGRADED TO AUDIT VERSION
    override val priority = 50

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[N3]"

        // 🏎️ Strategy 1: Racing
        println(EngineAnsi.CYAN + "      🏁 $prefix [RACE] Getting FX Rate..." + EngineAnsi.RESET)
        fxRacer.execute(packet, context)

        // 🛍️ Strategy 2: Scatter-Gather
        println(EngineAnsi.CYAN + "      🛍️ $prefix [SCATTER-GATHER] Calculating Best Fee..." + EngineAnsi.RESET)
        feeShopper.execute(packet, context)

        // 🔐 Strategy 3: Persistence & Transactionality
        val accountIdStr = context.getString("Account_ID")
        val accountId = accountIdStr?.toLongOrNull()
        var amount = BigDecimal.ZERO

        if (accountId != null) {
            val secretMoney = SecretContext.withdraw(accountId)

            if (secretMoney != null && secretMoney > BigDecimal.ZERO) {
                amount = secretMoney

                // 🟢 2. AUDIT: Record that we are attempting to pay
                auditor.recordAttempt(amount)

                try {
                    generatePdfAndNotify(prefix, accountId, amount)

                    // 🟢 3. AUDIT: Record Success
                    auditor.recordSuccess(amount)

                } catch (e: Exception) {
                    println(EngineAnsi.YELLOW + "      ↩️ $prefix Transaction Failed! Rolling back..." + EngineAnsi.RESET)

                    // ⚖️ Rollback
                    SecretContext.deposit(accountId, amount)

                    // 🟢 4. AUDIT: Record Failure
                    auditor.recordFailure()
                    throw e
                }
            } else {
                println(EngineAnsi.RED + "      ⛔ $prefix Skipped: Vault is empty." + EngineAnsi.RESET)
            }
        }
    }

    private fun generatePdfAndNotify(prefix: String, accountId: Long, amount: BigDecimal) {
        println(EngineAnsi.CYAN + "      📄 $prefix Generating Tax Certificate (50 Tawi)..." + EngineAnsi.RESET)

        // 🟢 DISABLE SABOTAGE FOR THE GOLDEN RUN
        val sabotage = false
        if (sabotage) {
            println(EngineAnsi.RED + "      🔥 $prefix CRITICAL ERROR: Printer caught fire!" + EngineAnsi.RESET)
            throw RuntimeException("Printer Fire")
        }

        val filename = "50Tawi_${UUID.randomUUID()}.pdf"
        println(EngineAnsi.GREEN + "      ✅ $prefix PDF Generated: $filename (Net: $amount THB)" + EngineAnsi.RESET)
        println(EngineAnsi.GREEN + "      📤 $prefix Sent to Printer Queue." + EngineAnsi.RESET)

        // 📢 Strategy 4: Broadcast
        println(EngineAnsi.CYAN + "      👉 $prefix Publishing Completion Event..." + EngineAnsi.RESET)
        publisher.publishEvent(PaymentCompletedEvent(this, accountId, amount))
    }

    override fun initialize(context: KernelContext) {
        auditor.reset()
    }
    override fun shutdown() {}
}