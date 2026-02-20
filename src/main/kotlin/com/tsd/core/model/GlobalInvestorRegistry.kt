package com.tsd.core.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "Global_Investor_Registry")
data class GlobalInvestorRegistry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GIN_ID")
    var ginId: Long? = null,

    @Column(name = "GIN_Code", nullable = false, length = 50)
    var ginCode: String,

    @Column(name = "Created_Date")
    var createdDate: LocalDateTime = LocalDateTime.now()
)