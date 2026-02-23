package com.tsd.features.distribution

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.Cartridge
import com.tsd.platform.spi.ExecutionContext
import org.springframework.stereotype.Component

@Component
class SendSmsNotificationCartridge : Cartridge {

    override val id: String = "Send_SMS_Notification"
    override val version: String = "1.0"
    override val priority: Int = 50 // 🟢 Added missing priority property!

    // 🟢 Added missing lifecycle methods!
    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val investorId = packet.data["INVESTOR_ID"] ?: "UNKNOWN"
        val amount = packet.data["AMOUNT"] ?: "0.00"

        println(EngineAnsi.CYAN + "   📱 [SMS_Gateway] Connecting to telecom provider..." + EngineAnsi.RESET)

        // Simulating a slow network call to an external SMS gateway (1.5 seconds)
        try {
            Thread.sleep(1500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        println(EngineAnsi.GREEN + "   ✉️ [SMS_Gateway] Message Sent! 'Dear $investorId, $amount THB has been credited to your account.'" + EngineAnsi.RESET)
    }

    override fun compensate(packet: ExchangePacket, context: ExecutionContext) {
        println(EngineAnsi.YELLOW + "   ⚠️ [SMS_Gateway] Cannot un-send an SMS. Notification already dispatched. Skipping compensation." + EngineAnsi.RESET)
    }
}