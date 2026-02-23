package com.tsd.core.port.output

import com.tsd.core.model.GlobalInvestorRegistry

interface GlobalInvestorRegistryPort {
    fun save(registry: GlobalInvestorRegistry): GlobalInvestorRegistry
}