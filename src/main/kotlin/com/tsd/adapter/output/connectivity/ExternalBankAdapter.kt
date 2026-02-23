package com.tsd.adapter.output.connectivity

import com.tsd.core.port.output.CashTransferPort
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ExternalBankAdapter : CashTransferPort {

    private val log = LoggerFactory.getLogger(ExternalBankAdapter::class.java)

    @CircuitBreaker(name = "externalBankApi", fallbackMethod = "fallbackTransfer")
    override fun initiateTransfer(accountId: String, amount: Double, simulateFail: Boolean): String {
        log.info("Attempting external network call to Bank API for Account: $accountId")

        // This is the logic that uses your 'fail' parameter from the test!
        if (simulateFail) {
            log.error("💥 Simulated Network Timeout! The bank is unresponsive.")
            throw RuntimeException("Simulated Bank API Down")
        }

        return "SUCCESS_TXN_ID_9999"
    }

    // THE FALLBACK METHOD (Must match the exact parameters of the original, plus the Exception)
    fun fallbackTransfer(accountId: String, amount: Double, simulateFail: Boolean, ex: Exception): String {
        log.warn("🛡️ CIRCUIT FALLBACK ENGAGED! Reason: ${ex.message}")
        return "QUEUED_FOR_RETRY"
    }
}