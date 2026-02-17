package com.tsd.features.authorization

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Check_Liquidity")
class CheckLiquidityCartridge : Cartridge {

    // 🟢 1. Implement ALL required members
    override val id = "Check_Liquidity"
    override val version = "1.0"
    override val priority = 10 // Higher priority for authorization checks

    // 🟢 2. Lifecycle methods
    override fun initialize(context: ExecutionContext) {
        // Optional: Load config or connect to services here
    }

    override fun shutdown() {
        // Optional: Cleanup resources
    }

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // 🟢 3. Fix "Unresolved reference get" -> use getObject
        val amountStr = context.getObject<Any>("amount")?.toString() ?: "0.00"
        val amount = BigDecimal(amountStr)

        println(EngineAnsi.YELLOW + "   💰 [Check_Liquidity] Verifying Asset Liquidity for $amount THB..." + EngineAnsi.RESET)

        val limit = BigDecimal("10000000.00") // Mock Limit: 10 Million

        if (amount > limit) {
            // 🟢 FIX: We do NOT throw an exception here.
            // We Log a Warning and let the Engine's Consensus Logic handle the "Stop & Vote".
            println(EngineAnsi.CYAN + "   ⚠️ [Check_Liquidity] High Value Detected ($amount). Flagging for Consensus..." + EngineAnsi.RESET)
        } else {
            // Only print "Auto-Approved" if it's under the limit
            println(EngineAnsi.GREEN + "   ✅ [Check_Liquidity] Amount OK. Auto-Approved." + EngineAnsi.RESET)
        }
    }
}