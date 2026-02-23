package com.tsd.adapter.output.persistence

import com.tsd.core.model.GlobalInvestorRegistry
import com.tsd.core.port.output.GlobalInvestorRegistryPort
import org.springframework.stereotype.Component

@Component
class GlobalInvestorRegistryPersistenceAdapter(
    private val repository: GlobalInvestorRegistryJpaRepository
) : GlobalInvestorRegistryPort {

    override fun save(registry: GlobalInvestorRegistry): GlobalInvestorRegistry {
        return repository.save(registry)
    }
}