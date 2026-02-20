package com.tsd.adapter.output.persistence

import com.tsd.core.model.GlobalInvestorRegistry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface GlobalInvestorRegistryJpaRepository : JpaRepository<GlobalInvestorRegistry, Long>