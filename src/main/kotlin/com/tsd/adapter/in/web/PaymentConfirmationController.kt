package com.tsd.adapter.`in`.web

import com.tsd.core.model.PaymentConfirmRequest
import com.tsd.core.model.PaymentRejectRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/payments")
class PaymentConfirmationController(private val jdbcTemplate: JdbcTemplate) {

    companion object {
        private val logger = LoggerFactory.getLogger(PaymentConfirmationController::class.java)
    }

    @PostMapping("/confirm")
    fun confirmPayment(@RequestBody request: PaymentConfirmRequest): ResponseEntity<String> {
        logger.info("Received confirmation for Payment ID: ${request.paymentId} with Ref: ${request.referenceNo}")

        val sql = """
            UPDATE dbo.Payment_Ledger 
            SET Payment_Status = 'CONFIRMED', 
                Calculation_Date = ? 
            WHERE Payment_ID = ? AND Payment_Status = 'PENDING'
        """.trimIndent()

        val rowsUpdated = jdbcTemplate.update(sql, LocalDateTime.now(), request.paymentId)

        return if (rowsUpdated > 0) {
            logger.info("Successfully settled Payment ID: ${request.paymentId}")
            ResponseEntity.ok("SUCCESS: Payment ${request.paymentId} confirmed.")
        } else {
            logger.warn("Settlement FAILED: Payment ID ${request.paymentId} not found or not in PENDING status.")
            ResponseEntity.status(404).body("ERROR: Payment not found or already processed.")
        }
    }

    @PostMapping("/reject")
    fun rejectPayment(@RequestBody request: PaymentRejectRequest): ResponseEntity<String> {
        logger.error("Payment REJECTED by Bank for ID: ${request.paymentId}. Reason: ${request.reason}")

        val sql = """
            UPDATE dbo.Payment_Ledger 
            SET Payment_Status = 'FAILED', 
                Calculation_Date = ?
            WHERE Payment_ID = ? AND Payment_Status = 'PENDING'
        """.trimIndent()

        val rowsUpdated = jdbcTemplate.update(sql, LocalDateTime.now(), request.paymentId)

        return if (rowsUpdated > 0) {
            ResponseEntity.ok("REJECTION LOGGED: Payment ${request.paymentId} marked as FAILED.")
        } else {
            ResponseEntity.status(404).body("ERROR: Payment not found.")
        }
    }
}