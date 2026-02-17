package com.tsd.adapter.out.persistence // 🟢 Updated Package

import com.tsd.platform.engine.util.EngineAnsi
import org.springframework.boot.CommandLineRunner
import org.springframework.core.annotation.Order  // 👈 THIS LINE IS REQUIRED
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
@Order(1) // 🟢 Run this FIRST (Priority 1)
class DatabaseHealthCheck(private val jdbcTemplate: JdbcTemplate) : CommandLineRunner {

    override fun run(vararg args: String?) {
        try {
            // 🧪 The Ultimate Test: Run a real SQL query
            jdbcTemplate.execute("SELECT 1")

            println("\n" + EngineAnsi.CYAN + """
                ===================================================
                ✅  DATABASE CONNECTION SUCCESSFUL (SQL Server)
                ===================================================
            """.trimIndent() + EngineAnsi.RESET + "\n")

        } catch (e: Exception) {
            println("\n" + EngineAnsi.RED + """
                ===================================================
                🔥  DATABASE CONNECTION FAILED!
                Reason: ${e.message}
                ===================================================
            """.trimIndent() + EngineAnsi.RESET + "\n")
        }
    }
}