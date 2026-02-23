package com.tsd.platform.spi

interface UniversalHttpPort {
    fun executeCall(traceId: String, url: String, method: String, payload: Map<String, Any>): Boolean
}