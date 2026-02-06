package com.tsd.platform.spi

interface PersistencePort {
    fun write(target: String, data: String)
    fun loadList(resourcePath: String): List<String>
}