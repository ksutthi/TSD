package com.tsd.platform.engine.util

import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

object SecretContext {
    private val vault = ConcurrentHashMap<Long, BigDecimal>()

    fun deposit(accountId: Long, amount: BigDecimal) {
        vault[accountId] = amount
    }

    fun withdraw(accountId: Long): BigDecimal? {
        return vault.remove(accountId)
}

    // 🟢 NEW: Bulk Fetch for Distribution Phase
    fun findAll(): Map<Long, BigDecimal> {
        // Return a copy so we don't hit concurrency issues during iteration
        return vault.toMap()
    }

    // 🟢 NEW: Clear specifically for bulk processing if needed
    fun clear(accountId: Long) {
        vault.remove(accountId)
    }
}