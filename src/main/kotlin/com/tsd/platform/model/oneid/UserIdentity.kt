package com.tsd.platform.model.oneid

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "User_Identity") // Maps to the Secondary DB (SET_One_ID)
data class UserIdentity(
    @Id
    @Column(name = "User_ID")
    val userId: String = "",

    @Column(name = "Full_Name")
    val fullName: String = "",

    @Column(name = "Role")
    val role: String = "USER", // ADMIN, TRADER, REGULATOR

    @Column(name = "Is_Active")
    val isActive: Boolean = true,

    @Column(name = "Last_Login")
    val lastLogin: LocalDateTime = LocalDateTime.now()
)