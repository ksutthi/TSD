package com.tsd.adapter.input.web

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class GodViewController(private val jdbcTemplate: JdbcTemplate) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/god-view/{gin}")
    fun getGlobalInvestorSnapshot(
        @PathVariable gin: String
        // REMOVED: masterKey parameter. The Filter already checked it!
    ): ResponseEntity<Any> {

        logger.info("GOD-VIEW ACCESS: Generating master summary for GIN: $gin")

        // Pillar 2: High-Performance Query
        val sql = "SELECT * FROM dbo.v_GodView_Investor_Summary WHERE GIN = ?"

        return try {
            val result = jdbcTemplate.queryForMap(sql, gin)
            ResponseEntity.ok(result)
        } catch (e: Exception) {
            // If the user doesn't exist, return a clean 404
            ResponseEntity.status(404).body("Investor $gin not found in Global Registry.")
        }
    }
}