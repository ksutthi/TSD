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

    // 🟢 NEW: The "Undo" Button (Saga Pattern)
    // If the workflow fails later, the Engine calls this to REVERSE the 'execute' action.
    // Example: If execute() did "Debit 50k", compensate() must do "Credit 50k".
    // Default: Do nothing (Safe for Read-Only steps like "Check Balance").
    fun compensate(packet: ExchangePacket, context: ExecutionContext) {
        // No-op by default
    }

    fun shutdown()
}