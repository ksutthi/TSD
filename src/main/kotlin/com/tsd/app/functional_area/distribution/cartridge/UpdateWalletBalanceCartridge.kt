package com.tsd.app.functional_area.distribution.cartridge

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component

@Component("Update_Wallet_Balance")
class UpdateWalletBalanceCartridge : Cartridge {
    override val id = "Update_Wallet_Balance"
    override val version = "1.0"
    override val priority = 1

    override fun execute(packet: ExchangePacket, context: KernelContext) {
        val net = packet.data["Net_Amount"]
        val accId = packet.data["Account_ID"]
        println("      💼 [N5] Crediting Wallet [$accId]: +$net THB")
    }
    override fun initialize(context: KernelContext) {}
    override fun shutdown() {}
}