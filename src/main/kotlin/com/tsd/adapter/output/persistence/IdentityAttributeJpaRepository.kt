package com.tsd.adapter.output.persistence

import com.tsd.core.model.IdentityAttribute
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface IdentityAttributeJpaRepository : JpaRepository<IdentityAttribute, Long> {

    @Query("""
        SELECT i.globalInvestorId 
        FROM IdentityAttribute i 
        WHERE i.attributeType = :type 
        AND i.attributeValue = :value
    """)
    fun findGinIdByTypeAndValue(
        @Param("type") type: String,
        @Param("value") value: String
    ): Long?
}