package com.tsd.features.portfolio

import com.tsd.core.model.AggregatedPosition
import com.tsd.core.model.GodViewQuery
import com.tsd.core.port.out.AccountBalancePort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GodViewAggregationCartridge(
    private val accountBalancePort: AccountBalancePort
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun getAggregatedPortfolio(query: GodViewQuery): List<AggregatedPosition> {
        val targetGinId = query.ginId ?: resolveIdentityQuietly(query.participantId, query.accountId)

        log.info("Executing God View Aggregation for GIN_ID: $targetGinId")

        val portfolio = accountBalancePort.aggregateBalancesByGin(targetGinId)

        log.info("Aggregation complete. Found ${portfolio.size} unique instruments.")
        return portfolio
    }

    private fun resolveIdentityQuietly(participantId: String?, accountId: String?): Long {
        require(participantId != null && accountId != null) {
            "REJECTED: Must provide either GIN_ID or both Participant ID and Account ID."
        }

        log.info("GIN_ID not provided. Resolving identity via Broker: $participantId, Account: $accountId")

        return accountBalancePort.findGinIdByBrokerAccount(participantId, accountId)
            ?: throw IllegalArgumentException("Identity Resolution Failed: No Global Record found for this Broker Account.")
    }
}