package com.tsd.app.functional_area.distribution.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import java.util.UUID

@Component("Gen_50Tawi_PDF")
class Gen50TawiPDFCartridge : Cartridge {

    override val id: String = "Gen_50Tawi_PDF"
    override val version: String = "1.0"
    override val priority: Int = 50 // Runs last in Module N

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   📄 [N3] Generating Tax Certificate (50 Tawi)...")

        // 1. Check Authorization
        val authStatus = packet.data["Auth_Status"] as? String ?: "Pending"

        if (authStatus == "Approved") {
            // 2. Simulate PDF Generation
            val fileId = UUID.randomUUID().toString()
            val fileName = "50Tawi_${fileId}.pdf"

            // 3. Save Link
            packet.data["Document_Link"] = "https://tsd.co.th/docs/$fileName"

            println("   ✅ [N3] PDF Generated: $fileName")
            println("   📤 [N3] Sent to Printer Queue.")
        } else {
            println("   ⛔ [N3] Skipped: Payment not authorized.")
        }
    }

    override fun shutdown() {}
}