package com.tsd.features.calculation

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import kotlinx.coroutines.*
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Fee_Calculator")
class FeeCalculatorCartridge : Cartridge {

    override val id = "Fee_Calculator"
    override val version = "2.0" // Scatter-Gather Version
    override val priority = 70

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[FEE]"
        println(EngineAnsi.CYAN + "      🛍️ $prefix Starting Scatter-Gather: Asking 3 Depts for Fees..." + EngineAnsi.RESET)

        val bestFee = runBlocking {
            findBestFee(prefix)
        }

        println(EngineAnsi.GREEN + "      ✅ $prefix SCATTER-GATHER RESULT: Lowest Fee Found = $bestFee THB" + EngineAnsi.RESET)

        // Save to packet
        packet.data["Final_Fee"] = bestFee
    }

    private suspend fun findBestFee(prefix: String): BigDecimal = coroutineScope {
        // 1. SCATTER: Launch 3 requests in parallel

        // 🏢 Standard Dept (Fast, Expensive)
        val taskStandard = async {
            delay(100)
            println(EngineAnsi.GRAY + "         🏢 $prefix Standard Dept: Quote 50.00 THB" + EngineAnsi.RESET)
            BigDecimal("50.00")
        }

        // 🏷️ Promo Dept (Slow, Cheap)
        val taskPromo = async {
            delay(2000) // Takes 2 seconds!
            println(EngineAnsi.YELLOW + "         🏷️ $prefix Promo Dept: Quote 30.00 THB" + EngineAnsi.RESET)
            BigDecimal("30.00")
        }

        // 👑 VIP Dept (Avg, Rejects)
        val taskVip = async {
            delay(500)
            // Logic: Not VIP
            println(EngineAnsi.RED + "         👑 $prefix VIP Dept: Customer not eligible (Reverted to 100.00)" + EngineAnsi.RESET)
            BigDecimal("100.00") // Penalty/High default
        }

        // 2. GATHER: Wait for ALL of them to finish
        // Unlike Racing, we do NOT cancel the others. We need all data.
        val allQuotes = awaitAll(taskStandard, taskPromo, taskVip)

        // 3. AGGREGATE: Pick the best (Lowest)
        val winner = allQuotes.minOrNull() ?: BigDecimal("50.00")
        return@coroutineScope winner
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}