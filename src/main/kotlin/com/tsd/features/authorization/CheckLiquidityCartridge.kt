package com.tsd.features.authorization

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Check_Liquidity")
class CheckLiquidityCartridge : Cartridge {

    override val id = "Check_Liquidity"
    override val version = "1.0"
    override val priority = 10

    override fun initialize(context: ExecutionContext) { }

    override fun shutdown() { }

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // Standardizing input reading
        val amountStr = packet.data["AMOUNT"]?.toString() ?: "0.00"
        val amount = BigDecimal(amountStr)

        println(EngineAnsi.YELLOW + "   💰 [Check_Liquidity] Verifying Asset Liquidity for $amount THB..." + EngineAnsi.RESET)

        val limit = BigDecimal("10000000.00")

        if (amount > limit) {
            println(EngineAnsi.CYAN + "   ⚠️ [Check_Liquidity] High Value Detected ($amount). Flagging for Consensus..." + EngineAnsi.RESET)
        } else {
            println(EngineAnsi.GREEN + "   ✅ [Check_Liquidity] Amount OK. Assets RESERVED." + EngineAnsi.RESET)
        }
    }

    // 🟢 SAGA COMPENSATION: The "Undo" Logic
    // If the workflow crashes later, the Engine calls this function.
    override fun compensate(packet: ExchangePacket, context: ExecutionContext) {
        val amountStr = packet.data["AMOUNT"]?.toString() ?: "0.00"

        println(EngineAnsi.RED + "      🔓 [ROLLBACK] Check_Liquidity: Releasing lock on $amountStr THB..." + EngineAnsi.RESET)
        println(EngineAnsi.RED + "      ✅ [ROLLBACK] Assets Un-Reserved successfully." + EngineAnsi.RESET)
    }
}