package com.tsd.app.distribution.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Random

@Component("Broadcast_Notification")
class BroadcastNotificationCartridge : Cartridge {

    override val id = "Broadcast_Notification"
    override val version = "3.1" // Clean Code Version
    override val priority = 40
    private val MAGENTA = "\u001B[35m"

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[NOTIFY]"

        // 1. Get Amount safely
        val rawAmount = packet.data["Net_Amount"]
        val amount: BigDecimal = when (rawAmount) {
            is BigDecimal -> rawAmount
            is Double -> BigDecimal.valueOf(rawAmount)
            is String -> BigDecimal(rawAmount)
            else -> BigDecimal.ZERO
        }

        println(EngineAnsi.CYAN + "      📡 $prefix notifying shareholder for Tx: ${packet.id}..." + EngineAnsi.RESET)

        val channels = listOf("Email", "SMS_Gateway")

        channels.forEach { channel ->
            try {
                if (channel == "SMS_Gateway") {
                    sendWithRetry(channel, amount)
                } else {
                    notifyChannel(channel, amount)
                }
            } catch (e: Exception) {
                println(EngineAnsi.RED + "         💀 $prefix [$channel] DIED after Retries." + EngineAnsi.RESET)
            }
        }
    }

    // 🟢 FIX: Actually using the 'amount' parameter in the log
    private fun sendWithRetry(channel: String, amount: BigDecimal) {
        val maxRetries = 3
        var attempt = 0
        var success = false

        while (attempt < maxRetries && !success) {
            attempt++
            try {
                // Now printing the amount!
                print("         📡 Contacting $channel for $amount THB (Attempt $attempt/$maxRetries)... ")

                simulateNetworkCall(channel)

                println(EngineAnsi.GREEN + "OK! Sent." + EngineAnsi.RESET)
                success = true

            } catch (e: Exception) {
                val waitTime = attempt * 100L
                println(EngineAnsi.YELLOW + "⚠️ Failed. Retrying..." + EngineAnsi.RESET)
                try { Thread.sleep(waitTime) } catch (ex: InterruptedException) {}
            }
        }
    }

    // 🟢 FIX: Actually using the 'amount' parameter in the log
    private fun notifyChannel(channel: String, amount: BigDecimal) {
        print("         📡 Contacting $channel regarding $amount THB... ")
        simulateNetworkCall(channel)
        println(EngineAnsi.GREEN + "OK! Sent." + EngineAnsi.RESET)
    }

    private fun simulateNetworkCall(channel: String) {
        try { Thread.sleep(50) } catch (e: InterruptedException) {}
        // Small chance of failure for SMS to test retry logic
        if (channel == "SMS_Gateway" && Random().nextInt(10) < 3) {
            throw RuntimeException("Gateway Timeout")
        }
    }

    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}