package com.tsd.features.authorization

import com.tsd.platform.engine.core.ConsensusService
import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Call_Identity_Mgmt")
class CallIdentityMgmtCartridge(
    private val consensusService: ConsensusService
) : Cartridge {

    override val id = "Call_Identity_Mgmt"
    override val version = "4.0"
    override val priority = 1

    // Trigger Consensus for amounts > 10 Million
    private val limit = BigDecimal("10000000.00")

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val prefix = "[CONSENSUS]"

        // 🟢 Robust conversion for Amount
        val rawAmount = packet.data["Net_Amount"]
        val netAmount = when (rawAmount) {
            is BigDecimal -> rawAmount
            is String -> BigDecimal(rawAmount)
            is Double -> BigDecimal.valueOf(rawAmount)
            is Int -> BigDecimal.valueOf(rawAmount.toLong())
            else -> BigDecimal.ZERO
        }

        println(EngineAnsi.CYAN + "      🛡️ $prefix Checking Value: $netAmount THB (Limit: $limit)..." + EngineAnsi.RESET)

        if (netAmount > limit) {
            println(EngineAnsi.YELLOW + "      ✋ HIGH VALUE DETECTED! ($netAmount > $limit)" + EngineAnsi.RESET)
            println(EngineAnsi.YELLOW + "         (Transaction is PAUSED. Waiting for 2 votes...)" + EngineAnsi.RESET)

            // 🟢 BLOCKING CALL: This line freezes execution for up to 60 seconds
            val approved = consensusService.waitForConsensus(
                txId = packet.id,
                amount = netAmount.toString()
            )

            if (approved) {
                println(EngineAnsi.GREEN + "      ✅ $prefix Votes Received. Manager Approved. Resuming..." + EngineAnsi.RESET)
            } else {
                println(EngineAnsi.RED + "      ❌ $prefix REJECTED. Timeout or Denied." + EngineAnsi.RESET)
                throw RuntimeException("Transaction Rejected by Consensus")
            }
        } else {
            println(EngineAnsi.GREEN + "      ✅ $prefix Amount OK. Auto-Approved." + EngineAnsi.RESET)
        }
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}