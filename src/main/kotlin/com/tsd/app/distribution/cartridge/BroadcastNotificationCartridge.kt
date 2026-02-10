package com.tsd.app.notification.cartridge

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.event.PaymentCompletedEvent
import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.KernelContext
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Random

@Component("Broadcast_Notification")
class BroadcastNotificationCartridge : Cartridge, ApplicationListener<PaymentCompletedEvent> {

    override val id = "Broadcast_Notification"
    override val version = "3.0" // Retry Version!
    override val priority = 100
    private val MAGENTA = "\u001B[35m"

    override fun onApplicationEvent(event: PaymentCompletedEvent) {
        val amount = event.amount
        val prefix = "[EVENT]"

        println(MAGENTA + "      📢 $prefix [BROADCAST] Town Crier: Announcing payout of $amount THB..." + EngineAnsi.RESET)

        val channels = listOf("Email", "SMS_Gateway", "Line_App")

        channels.forEach { channel ->
            try {
                if (channel == "SMS_Gateway") {
                    // 🟢 USE RETRY LOGIC FOR SMS
                    sendWithRetry(channel, amount)
                } else {
                    // Normal Send for others
                    notifyChannel(channel, amount)
                }
            } catch (e: Exception) {
                println(EngineAnsi.RED + "         💀 $prefix [$channel] DIED after Retries. Giving up." + EngineAnsi.RESET)
            }
        }
        println(MAGENTA + "      ✅ $prefix [BROADCAST] Finished." + EngineAnsi.RESET)
    }

    // 🟢 THE RETRY STRATEGY
    private fun sendWithRetry(channel: String, amount: BigDecimal) {
        val maxRetries = 3
        var attempt = 0
        var success = false

        while (attempt < maxRetries && !success) {
            attempt++
            try {
                print("         📡 Contacting $channel (Attempt $attempt/$maxRetries)... ")

                // Simulate Network Flakiness
                simulateNetworkCall(channel)

                println(EngineAnsi.GREEN + "OK! Sent." + EngineAnsi.RESET)
                success = true

            } catch (e: Exception) {
                // If failed, wait and loop again
                val waitTime = attempt * 500L // Linear Backoff (500ms, 1000ms, 1500ms)
                println(EngineAnsi.YELLOW + "⚠️ Failed (${e.message}). Retrying in ${waitTime}ms..." + EngineAnsi.RESET)
                try { Thread.sleep(waitTime) } catch (ex: InterruptedException) {}
            }
        }

        if (!success) {
            throw RuntimeException("Max Retries Exceeded")
        }
    }

    private fun notifyChannel(channel: String, amount: BigDecimal) {
        print("         📡 Contacting $channel... ")
        simulateNetworkCall(channel)
        println(EngineAnsi.GREEN + "OK! Sent." + EngineAnsi.RESET)
    }

    private fun simulateNetworkCall(channel: String) {
        try { Thread.sleep(200) } catch (e: InterruptedException) {}

        if (channel == "SMS_Gateway") {
            // 🎲 70% Chance of Failure to force retries
            // Eventually (statistically) it might pass on the 2nd or 3rd try!
            if (Random().nextInt(10) < 7) {
                throw RuntimeException("504 Gateway Time-out")
            }
        }
    }

    override fun execute(packet: ExchangePacket, context: KernelContext) {}
    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}