package com.tsd.core.port.out

import com.tsd.core.model.GlobalInvestorRegistry

interface GlobalInvestorRegistryPort {
    fun save(registry: GlobalInvestorRegistry): GlobalInvestorRegistry
}