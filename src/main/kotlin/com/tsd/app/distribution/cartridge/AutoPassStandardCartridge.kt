package com.tsd.app.distribution.cartridge

import com.tsd.platform.model.registry.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component("Auto_Pass_Standard")
class AutoPassStandardCartridge : Cartridge {

    override val id: String = "Auto_Pass_Standard"
    override val version: String = "1.0"
    override val priority: Int = 10

    override fun initialize(context: KernelContext) {}

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        println("   🛡️ [N1] Checking Authorization Limits...")

        // 1. Get the Net Amount (Calculated by M2 Tax Engine)
        val netAmount = packet.data["Net_Amount"] as? Double ?: 0.0
        val threshold = 1_000_000.0 // 1 Million THB Limit

        // 2. Check Rule
        if (netAmount <= threshold) {
            println("   ✅ [N1] Amount ($netAmount) is within auto-approval limits.")
            packet.data["Auth_Status"] = "Approved"
        } else {
            println("   ⚠️ [N1] Amount ($netAmount) exceeds limit! Flagging for Manual Review.")
            packet.data["Auth_Status"] = "Pending_Review"
        }
    }

    override fun shutdown() {}
}