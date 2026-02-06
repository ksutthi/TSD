package com.tsd.app.functional_area.account.model

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class AccountBalanceKey(
    @Column(name = "Participant_ID")
    val participantId: Int = 0,

    @Column(name = "Account_ID")
    val accountId: Long = 0,

    @Column(name = "Instrument_ID")
    val instrumentId: Int = 0,

    @Column(name = "Snapshot_Date")
    val snapshotDate: LocalDate = LocalDate.now(),

    @Column(name = "Registrar_ID")
    val registrarId: Int = 1
) : Serializable