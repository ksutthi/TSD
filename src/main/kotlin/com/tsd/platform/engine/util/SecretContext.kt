package com.tsd.platform.engine.util

import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

// 🔒 A static vault that exists outside the framework's control
object SecretContext {
    // Maps Account_ID -> Net_Amount
    private val vault = ConcurrentHashMap<Long, BigDecimal>()

    fun deposit(accountId: Long, amount: BigDecimal) {
        vault[accountId] = amount
    }

    fun withdraw(accountId: Long): BigDecimal? {
        return vault[accountId]
    }
}