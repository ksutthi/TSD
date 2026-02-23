package com.tsd.core.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.math.BigDecimal

@Entity
@Table(name = "User_Identity") // Maps to the Secondary DB (SET_One_ID)
data class UserIdentity(
    @Id
    @Column(name = "User_ID")
    val userId: String = "",

    @Column(name = "Full_Name")
    val fullName: String = "",

    @Column(name = "Role")
    val role: String = "USER", // ADMIN, TRADER, REGULATOR, HEAD_OF_OPERATIONS

    @Column(name = "Is_Active")
    val isActive: Boolean = true,

    @Column(name = "Last_Login")
    val lastLogin: LocalDateTime = LocalDateTime.now(),

    // --- Added for External Policy & Risk Matrix ---
    @Column(name = "Broker_Code")
    val brokerCode: String = "UNKNOWN",

    @Column(name = "Approval_Limit")
    val approvalLimit: BigDecimal = BigDecimal.ZERO
) {
    /**
     * Helper method for the Policy Engine Port to check permissions
     */
    fun hasRole(checkRole: String): Boolean {
        return this.role.equals(checkRole, ignoreCase = true)
    }
}