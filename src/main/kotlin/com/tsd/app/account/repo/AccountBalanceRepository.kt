package com.tsd.app.account.repo

import com.tsd.app.account.model.AccountBalance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountBalanceRepository : JpaRepository<AccountBalance, Long>