package com.tsd.core.port.output

interface CashTransferPort {
    // I added the simulateFail parameter here to fix the unused variable error
    fun initiateTransfer(accountId: String, amount: Double, simulateFail: Boolean): String
}