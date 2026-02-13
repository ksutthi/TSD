package com.tsd.app.authorization.cartridge

import com.tsd.platform.engine.core.ConsensusService // 🟢 REQUIRED FOR PAUSE
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Call_Identity_Mgmt")
class CallIdentityMgmtCartridge(
    private val consensusService: ConsensusService // 🔌 Inject the Service
) : Cartridge {

    override val id = "Call_Identity_Mgmt"
    override val version = "4.0"
    override val priority = 1

    // 🟢 Lower limit to 10M so the 25M Billionaire triggers it!
    private val limit = BigDecimal("10000000.00")

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = "[CONSENSUS]"

        // 1. Get the Net Amount (Injected by EventService or Calculator)
        val netAmount = packet.data["Net_Amount"] as? BigDecimal ?: BigDecimal.ZERO

        println(EngineAnsi.CYAN + "      🛡️ $prefix Checking Value: $netAmount THB (Limit: $limit)..." + EngineAnsi.RESET)

        if (netAmount > limit) {
            // 🚨 TRIGGER THE PAUSE
            println(EngineAnsi.YELLOW + "      ✋ HIGH VALUE DETECTED! ($netAmount > $limit)" + EngineAnsi.RESET)

            // 🟢 CALL THE CORRECT METHOD NAME HERE: waitForConsensus
            val approved = consensusService.waitForConsensus(
                txId = packet.id,
                amount = netAmount.toString()
            )

            if (approved) {
                println(EngineAnsi.GREEN + "      ✅ $prefix Approvals Received. Resuming Transaction..." + EngineAnsi.RESET)
            } else {
                println(EngineAnsi.RED + "      ❌ $prefix Transaction REJECTED by Consensus (Timeout)." + EngineAnsi.RESET)
                throw RuntimeException("Transaction Rejected") // Stop the flow
            }
        } else {
            println(EngineAnsi.GREEN + "      ✅ $prefix Amount within limits. Auto-Approved." + EngineAnsi.RESET)
        }
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}