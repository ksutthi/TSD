package com.tsd.app.account.service

import com.tsd.app.account.model.AccountBalance
import com.tsd.app.account.repo.AccountBalanceRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

@Component
class DataLoader(val repo: AccountBalanceRepository) : CommandLineRunner {

    override fun run(args: Array<String>) {
        println("🧹 FLUSHING VAULT: Deleting all existing accounts...")
        repo.deleteAll() // <--- 🛑 RESET THE DB EVERY TIME

        println("🌱 Seeding Database with Fresh Mock Data...")

        // 1. The Billionaire (Account 5) - Triggers CONSENSUS
        val billionaire = AccountBalance(
            accountId = 5,
            participantId = 999,
            instrumentId = 1, // PTT
            quantity = BigDecimal("5000000.00"), // 5 Million Shares
            snapshotDate = LocalDate.now()
        )

        // 2. A Regular Joe (Account 1) - Triggers AUTO-APPROVE
        val regularJoe = AccountBalance(
            accountId = 1,
            participantId = 999,
            instrumentId = 1, // PTT
            quantity = BigDecimal("100.00"), // 100 Shares
            snapshotDate = LocalDate.now()
        )

        repo.saveAll(listOf(billionaire, regularJoe))
        println("💰 Billionaire (Account 5) inserted! [5,000,000 Shares]")
    }
}