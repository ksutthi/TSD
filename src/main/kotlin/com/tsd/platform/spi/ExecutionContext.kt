package com.tsd.platform.spi

import java.math.BigDecimal

interface ExecutionContext {
    fun getTenantID(): String
    fun getEventID(): String

    // Data Accessors
    fun getString(key: String): String
    fun getAmount(key: String): BigDecimal
    fun <T> getObject(key: String): T?

    // Data Mutators
    fun set(key: String, value: Any)

    // Operations
    fun log(tag: String, message: String)
    fun getConfig(key: String): String
    fun abort(reason: String)
}