package com.tsd.platform.spi

/**
 * 🔌 Cartridge Interface
 * Defines the contract for all business logic plugins.
 *
 * NOTE: This file relies ONLY on other SPI classes (ExchangePacket, ExecutionContext).
 * It does NOT import the Engine implementation (KernelContext).
 */
interface Cartridge {
    val id: String
    val version: String
    val priority: Int

    // 🟢 Fixed Typo: context is now 'ExecutionContext' (The Interface)
    fun initialize(context: ExecutionContext)

    // 🟢 Decoupled: execute now receives 'ExecutionContext', not 'KernelContext'
    fun execute(packet: ExchangePacket, context: ExecutionContext)

    fun shutdown()
}