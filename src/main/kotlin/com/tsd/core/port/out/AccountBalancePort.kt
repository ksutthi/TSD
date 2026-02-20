package com.tsd.core.port.out

import com.tsd.core.model.AggregatedPosition

interface AccountBalancePort {
    fun findGinIdByBrokerAccount(participantId: String, accountId: String): Long?
    fun aggregateBalancesByGin(ginId: Long): List<AggregatedPosition>

    // 🟢 NEW: The Isolated Query enforcing horizontal and vertical walls
    fun aggregateBalancesWithIsolation(ginId: Long, participantId: Int?, registrarId: Int?): List<AggregatedPosition>
}