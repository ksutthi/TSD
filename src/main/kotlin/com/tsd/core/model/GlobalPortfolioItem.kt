package com.tsd.core.model

import java.math.BigDecimal

/**
 * This class maps exactly to the result of "usp_GetGlobalPortfolio".
 * It represents ONE row in that result table.
 */
data class GlobalPortfolioItem(
    val registrarCode: String,      // e.g., "TSD", "BOT"
    val participantName: String,    // e.g., "Kiatnakin Phatra"
    val accountId: String,          // e.g., "ACC-TSD-01"
    val symbol: String,             // e.g., "TBEV"
    val isin: String?,              // e.g., "TH123..."
    val totalUnits: BigDecimal,     // Use BigDecimal for money/shares! Never Double.
    val balanceStatus: String,      // e.g., "ACTIVE", "FROZEN"
    val walletStatus: String        // e.g., "ACTIVE"
)