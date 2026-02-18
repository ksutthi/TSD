package com.tsd.adapter.output.persistence

import com.tsd.core.model.AccountBalance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountBalanceRepository : JpaRepository<AccountBalance, Long>