package com.tsd.features.distribution

import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component("Update_Wallet_Balance")
class UpdateWalletBalanceCartridge : Cartridge {
    override val id = "Update_Wallet_Balance"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
        val net = packet.data["Net_Amount"]
        val accId = packet.data["Account_ID"]
        println("      💼 [N5] Crediting Wallet [$accId]: +$net THB")
    }
    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}