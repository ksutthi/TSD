package com.tsd.core.model

import jakarta.persistence.*
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "AccountBalances")
// 🟢 NEW: The Matrix Access Control Filters
@FilterDef(name = "participantFilter", parameters = [ParamDef(name = "partId", type = Int::class)])
@Filter(name = "participantFilter", condition = "Participant_ID = :partId")
@FilterDef(name = "registrarFilter", parameters = [ParamDef(name = "regId", type = Int::class)])
@Filter(name = "registrarFilter", condition = "Registrar_ID = :regId")
data class AccountBalance(
    @Id
    @Column(name = "Account_ID")
    var accountId: Long = 0,

    @Column(name = "Participant_ID")
    var participantId: Int = 0,

    @Column(name = "Instrument_ID")
    var instrumentId: Int = 0,

    @Column(name = "Snapshot_Date")
    var snapshotDate: LocalDate = LocalDate.now(),

    @Column(name = "Registrar_ID")
    var registrarId: Int = 0,

    // 🟢 NEW: The Tier 1 Anchor (Mapped to V3 Flyway Script)
    @Column(name = "GIN_ID")
    var globalInvestorId: Long? = null,

    @Column(name = "Quantity")
    var quantity: BigDecimal = BigDecimal.ZERO,

    @Column(name = "Last_Updated")
    var lastUpdated: LocalDateTime = LocalDateTime.now()
) {
    // 🟢 HELPERS (Logic that lives on the data - untouched)
    val paymentMode: String get() = if (accountId % 2 != 0L) "SWIFT" else "BAHTNET"
    val taxProfile: String get() = "Standard_Individual"
    val countryCode: String get() = "TH"
    val sourceSystem: String get() = "CSD"
    val isEligible: Boolean get() = true
    val nationalId: String get() = "NAT-ID-${accountId}"
    val fullName: String get() = "Account Holder #$accountId"
    val shareBalance: BigDecimal get() = quantity
}