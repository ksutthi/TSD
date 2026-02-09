package com.tsd.app.account.service

import com.tsd.app.account.repo.AccountBalanceRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PortfolioService(
    private val repository: AccountBalanceRepository,
    private val entityManager: EntityManager
) {
    @Transactional
    fun getMyPortfolio(participantId: Int): List<Any> {
        // 1. SET IDENTITY (Security Handshake)
        val query = entityManager.createNativeQuery("EXEC sp_set_session_context @key=N'ParticipantID', @value=:id")
        query.setParameter("id", participantId)
        query.executeUpdate()

        // 2. FETCH DATA (RLS Protected)
        return repository.findAll()
    }
}