package com.tsd.adapter.output.persistence

import com.tsd.adapter.out.persistence.AccountBalanceJpaRepository
import com.tsd.core.model.AggregatedPosition
import com.tsd.core.port.out.AccountBalancePort
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AccountBalancePersistenceAdapter(
    private val repository: AccountBalanceJpaRepository,
    private val entityManager: EntityManager // 🟢 Injected to control Matrix Filters
) : AccountBalancePort {

    override fun findGinIdByBrokerAccount(participantId: String, accountId: String): Long? {
        return repository.findGinIdByBrokerAccount(participantId.toInt(), accountId.toLong())
    }

    override fun aggregateBalancesByGin(ginId: Long): List<AggregatedPosition> {
        return repository.aggregateBalancesByGin(ginId)
    }

    // 🟢 NEW: Activate Row-Level Security
    @Transactional(readOnly = true)
    override fun aggregateBalancesWithIsolation(ginId: Long, participantId: Int?, registrarId: Int?): List<AggregatedPosition> {
        val session = entityManager.unwrap(Session::class.java)

        // 1. Participant Angle (Horizontal Wall)
        if (participantId != null) {
            session.enableFilter("participantFilter").setParameter("partId", participantId)
        }

        // 2. Registrar Angle (Vertical Wall)
        if (registrarId != null) {
            session.enableFilter("registrarFilter").setParameter("regId", registrarId)
        }

        try {
            // Hibernate intercepts this standard query and physically injects the WHERE clauses
            return repository.aggregateBalancesByGin(ginId)
        } finally {
            // CRITICAL: Clean up the session context to prevent connection pool leaks
            if (participantId != null) session.disableFilter("participantFilter")
            if (registrarId != null) session.disableFilter("registrarFilter")
        }
    }
}