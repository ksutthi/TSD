package com.tsd.features.market

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Random

@Component("Fx_Market_Rates") // 🟢 Specific Bean Name matching ID
class FxRateCartridge : Cartridge {

    override val id: String = "Fx_Market_Rates"
    override val version: String = "2.0" // Upgraded Version!
    override val priority: Int = 50

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[FX]"
        println(EngineAnsi.CYAN + "      💱 $prefix Starting FX Rate Race (USD/THB)..." + EngineAnsi.RESET)

        // 🟢 THE RACE: Run inside a blocking scope (Sync wrapper for Async logic)
        val winningRate = runBlocking {
            raceForRates(prefix)
        }

        println(EngineAnsi.GREEN + "      🏆 $prefix RACE FINISHED! Winner Rate: $winningRate" + EngineAnsi.RESET)

        // Save the winner to the packet (for downstream use)
        packet.data["FX_Rate"] = winningRate
        context.set("FX_Rate", winningRate)
    }

    // 🏎️ The Racing Logic (Fastest Wins)
    private suspend fun raceForRates(prefix: String): BigDecimal = coroutineScope {

        // 🐢 Racer 1: Provider A (Slow)
        val providerA = async {
            delay(3000) // Takes 3 seconds
            println(EngineAnsi.GRAY + "         🐢 $prefix Provider A (Slow) arrived late..." + EngineAnsi.RESET)
            BigDecimal("34.50")
        }

        // 🐇 Racer 2: Provider B (Fast)
        val providerB = async {
            val jitter = Random().nextLong(500)
            delay(500 + jitter) // Takes ~0.5 seconds
            println(EngineAnsi.YELLOW + "         🐇 $prefix Provider B (Fast) crossing finish line!" + EngineAnsi.RESET)
            BigDecimal("34.55")
        }

        // 🐕 Racer 3: Provider C (Average)
        val providerC = async {
            delay(1500) // Takes 1.5 seconds
            println(EngineAnsi.GRAY + "         🐕 $prefix Provider C (Avg) arrived." + EngineAnsi.RESET)
            BigDecimal("34.52")
        }

        // 🏁 The Select: Returns the result of the FIRST one to finish
        val winner = select<BigDecimal> {
            providerA.onAwait { it }
            providerB.onAwait { it }
            providerC.onAwait { it }
        }

        // 🛑 Optimization: Cancel the losers! We don't wait for them.
        providerA.cancel()
        providerB.cancel()
        providerC.cancel()

        return@coroutineScope winner
    }

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}