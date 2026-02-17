package com.tsd.adapter.`in`.web

import org.springframework.web.bind.annotation.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.http.ResponseEntity
import org.slf4j.LoggerFactory
import com.tsd.core.model.Holding
import com.tsd.core.model.InvestorStatement
import com.tsd.core.model.PaymentRecord

@RestController
@RequestMapping("/api/investor")
class InvestorStatementController(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/{gin}/statement")
    fun getStatement(@PathVariable gin: String): ResponseEntity<InvestorStatement> {
        logger.info("Generating consolidated statement for GIN: $gin")

        // 1. Resolve Name via our Global View (The Bridge)
        val nameSql = "SELECT TOP 1 Investor_Name FROM dbo.v_Investor_Consolidated_Portfolio WHERE GIN = ?"
        val investorName = try {
            jdbcTemplate.queryForObject(nameSql, String::class.java, gin) ?: "UNKNOWN INVESTOR"
        } catch (e: Exception) {
            "UNKNOWN INVESTOR"
        }

        // 2. Fetch all Holdings (Equities, Bonds, etc.)
        val holdingsSql = """
            SELECT ISIN, Instrument_Type, Total_Units 
            FROM dbo.v_Investor_Consolidated_Portfolio 
            WHERE GIN = ?
        """.trimIndent()

        val holdings = jdbcTemplate.query(holdingsSql, { rs, _ ->
            Holding(
                isin = rs.getString("ISIN"),
                type = rs.getString("Instrument_Type"),
                units = rs.getDouble("Total_Units")
            )
        }, gin)

        // 3. Fetch Payout History
        val paymentsSql = """
            SELECT ISIN, Net_Amount, Payment_Status, Calculation_Date 
            FROM dbo.Payment_Ledger 
            WHERE GIN = ?
            ORDER BY Calculation_Date DESC
        """.trimIndent()

        val payments = jdbcTemplate.query(paymentsSql, { rs, _ ->
            PaymentRecord(
                isin = rs.getString("ISIN"),
                amount = rs.getDouble("Net_Amount"),
                status = rs.getString("Payment_Status"),
                date = rs.getString("Calculation_Date")
            )
        }, gin)

        return ResponseEntity.ok(InvestorStatement(gin, investorName, holdings, payments))
    }
}