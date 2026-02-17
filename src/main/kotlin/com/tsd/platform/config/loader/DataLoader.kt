package com.tsd.platform.config.loader

import com.tsd.adapter.out.persistence.AccountBalanceRepository
import com.tsd.core.model.AccountBalance
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 🏗️ DATA LOADER (Seed Data)
 * Resets and pre-fills the database with test accounts for development.
 * 🛡️ SAFETY: This only runs when the profile is NOT 'prod'.
 */
@Component
@Profile("!prod") // 🛑 SAFETY GUARD: Prevents accidental wipe in Production
class DataLoader(val repo: AccountBalanceRepository) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataLoader::class.java)

    override fun run(args: Array<String>) {
        logger.warn("==================================================")
        logger.warn("🧹 [DataLoader] FLUSHING VAULT: Deleting all existing accounts...")
        repo.deleteAll()

        logger.info("🌱 [DataLoader] Seeding Database with Fresh Mock Data...")

        // 1. The Billionaire (Account 5) - Designed to Trigger 'CONSENSUS'
        // Strategy: Quantity > 1,000,000 requires high-value approval
        val billionaire = AccountBalance(
            accountId = 5,
            participantId = 999,
            instrumentId = 1, // PTT
            quantity = BigDecimal("5000000.00"), // 5 Million Shares
            snapshotDate = LocalDate.now()
        )

        // 2. A Regular Joe (Account 1) - Designed to Trigger 'AUTO-APPROVE'
        // Strategy: Small quantity passes through automatically
        val regularJoe = AccountBalance(
            accountId = 1,
            participantId = 999,
            instrumentId = 1, // PTT
            quantity = BigDecimal("100.00"), // 100 Shares
            snapshotDate = LocalDate.now()
        )

        repo.saveAll(listOf(billionaire, regularJoe))

        logger.info("✅ [DataLoader] SUCCESS: Inserted 2 Mock Accounts.")
        logger.info("   -> Account 5: 5,000,000 Shares (Test High Value)")
        logger.info("   -> Account 1:       100 Shares (Test Auto Pass)")
        logger.warn("==================================================")
    }
}