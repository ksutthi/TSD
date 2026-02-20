package com.tsd.adapter.out.persistence

import com.tsd.core.model.AccountBalance
import com.tsd.core.model.AggregatedPosition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface AccountBalanceJpaRepository : JpaRepository<AccountBalance, Long> {

    // 1. Find the GIN_ID based on the Broker Account (Types matched to Entity)
    @Query("""
        SELECT a.globalInvestorId 
        FROM AccountBalance a 
        WHERE a.participantId = :participantId 
          AND a.accountId = :accountId
    """)
    fun findGinIdByBrokerAccount(participantId: Int, accountId: Long): Long?

    // 2. The God View: Aggregate all shares across all brokers
    @Query("""
        SELECT new com.tsd.core.model.AggregatedPosition(a.instrumentId, SUM(a.quantity)) 
        FROM AccountBalance a 
        WHERE a.globalInvestorId = :ginId 
        GROUP BY a.instrumentId
    """)
    fun aggregateBalancesByGin(ginId: Long): List<AggregatedPosition>
}