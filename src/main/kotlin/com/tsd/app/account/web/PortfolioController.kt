package com.tsd.app.account.web

import com.tsd.app.account.model.Snapshot
import com.tsd.app.account.model.SnapshotId
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/gateway")
class PortfolioController {

    // 2. SIMULATED DATABASE
    // This now uses the classes you defined in Snapshot.kt
    private val database = listOf(
        // User 101 (Hedge Fund)
        Snapshot(
            SnapshotId(101, "TSLA-STOCK", 1),
            "ACC-101-9988",
            "Tesla Common Stock",
            "Acme Hedge Fund",
            500,
            "2026-01-30T10:00:00"
        ),
        Snapshot(
            SnapshotId(101, "AAPL-BOND", 1),
            "ACC-101-9988",
            "Apple Corp Bond 5%",
            "Acme Hedge Fund",
            200,
            "2026-01-30T10:05:00"
        ),

        // User 201 (J.P. Morgan)
        Snapshot(
            SnapshotId(201, "USD-CASH", 1),
            "JPM-L-777001",
            "US Dollar Liquidity",
            "J.P. Morgan Chase",
            9_000_000,
            "2026-01-30T09:00:00"
        ),
        Snapshot(
            SnapshotId(201, "EUR-BOND", 2),
            "JPM-F-555002",
            "German Gov Bond 10Y",
            "J.P. Morgan Chase",
            50_000,
            "2026-01-30T09:30:00"
        ),

        // User 202 (Goldman Sachs)
        Snapshot(
            SnapshotId(202, "GOOGL-EQ", 1),
            "GS-EQ-112233",
            "Alphabet Class A",
            "Goldman Sachs",
            750,
            "2026-01-30T11:00:00"
        )
    )

    @GetMapping("/my-portfolio")
    fun getMyPortfolio(
        @RequestHeader("X-Participant-ID", required = false) participantId: Int?,
        @RequestHeader("X-Registrar-ID", required = false) registrarId: Int?,
        @RequestHeader("X-God-Mode", required = false) godMode: Boolean?
    ): List<Snapshot> {

        if (godMode == true) return database
        if (registrarId != null) return database.filter { it.id.registrarId == registrarId }
        if (participantId != null) return database.filter { it.id.participantId == participantId }

        return emptyList()
    }
}