package com.tsd.platform.spi

import com.tsd.platform.model.ExchangePacket
import com.tsd.platform.spi.KernelContext

interface Cartridge {
    val id: String
    val version: String
    val priority: Int

    fun initialize(context: KernelContext)
    fun execute(packet: ExchangePacket, context: KernelContext)
    fun shutdown()
}