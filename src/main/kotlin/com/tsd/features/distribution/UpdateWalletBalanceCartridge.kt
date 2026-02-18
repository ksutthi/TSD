package com.tsd.features.distribution

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component("Update_Wallet_Balance")
class UpdateWalletBalanceCartridge : Cartridge {

    override val id = "Update_Wallet_Balance"
    override val version = "1.0"
    override val priority = 1

    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        // 1. Safe Data Extraction (Handles Double, BigDecimal, String)
        val rawAmount = packet.data["Net_Amount"] ?: packet.data["AMOUNT"]
        val amount: BigDecimal = when (rawAmount) {
            is BigDecimal -> rawAmount
            is Double -> BigDecimal.valueOf(rawAmount)
            is String -> BigDecimal(rawAmount)
            is Int -> BigDecimal.valueOf(rawAmount.toLong())
            else -> BigDecimal.ZERO
        }

        val accId = packet.data["Account_ID"] ?: packet.data["WALLET_ID"] ?: "UNKNOWN"

        // 🟢 CHAOS MONKEY: Simulate Crash if amount is 999.00
        if (amount.compareTo(BigDecimal("999.00")) == 0) {
            println(EngineAnsi.MAGENTA + "      ⚡ [Update_Wallet] Connecting to Bank API..." + EngineAnsi.RESET)
            try { Thread.sleep(500) } catch (_: Exception) {}
            throw RuntimeException("Connection Refused: Bank Gateway Timeout (504)")
        }

        println(EngineAnsi.GREEN + "      💼 [N5] Update_Wallet_Balance: Credited $accId with +$amount THB" + EngineAnsi.RESET)
    }

    // 🟢 SAGA COMPENSATION: The "Undo" Button
    override fun compensate(packet: ExchangePacket, context: ExecutionContext) {
        val accId = packet.data["Account_ID"] ?: packet.data["WALLET_ID"] ?: "UNKNOWN"
        val rawAmount = packet.data["Net_Amount"] ?: packet.data["AMOUNT"]

        println(EngineAnsi.RED + "      🔙 [Undo] Update_Wallet_Balance: Reversing credit for $accId ($rawAmount THB)..." + EngineAnsi.RESET)
    }
}