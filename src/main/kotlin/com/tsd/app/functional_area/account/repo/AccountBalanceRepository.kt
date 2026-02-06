package com.tsd.app.functional_area.account.repo

import com.tsd.app.functional_area.account.model.AccountBalance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountBalanceRepository : JpaRepository<AccountBalance, Long>