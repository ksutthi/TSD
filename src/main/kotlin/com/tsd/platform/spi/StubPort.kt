package com.tsd.platform.spi

interface StubPort {
    fun send(destination: String, payload: String)
    fun receive(source: String): String = ""
}