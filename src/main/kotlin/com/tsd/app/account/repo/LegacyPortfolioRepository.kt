package com.tsd.app.account.repo

import com.microsoft.sqlserver.jdbc.SQLServerCallableStatement
import com.microsoft.sqlserver.jdbc.SQLServerConnection
import com.microsoft.sqlserver.jdbc.SQLServerDataTable
import com.tsd.app.account.model.BatchAccountResult
import com.tsd.app.account.model.GlobalPortfolioItem
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Types

@Repository
class LegacyPortfolioRepository(private val jdbcTemplate: JdbcTemplate) {

    // 1. READ: Get Portfolio
    fun getGlobalPortfolio(globalEntityId: String, requestingUserId: Int): List<GlobalPortfolioItem> {
        val sql = "EXEC usp_GetGlobalPortfolio @GlobalEntityId = ?, @RequestingUserId = ?"

        return jdbcTemplate.query(sql, { rs: ResultSet, _: Int ->
            GlobalPortfolioItem(
                registrarCode = rs.getString("RegistrarCode"),
                participantName = rs.getString("ParticipantName"),
                accountId = rs.getString("AccountId"),
                symbol = rs.getString("Symbol"),
                isin = rs.getString("ISIN"),
                totalUnits = rs.getBigDecimal("TotalUnits"),
                balanceStatus = rs.getString("BalanceStatus"),
                walletStatus = rs.getString("WalletStatus")
            )
        }, globalEntityId, requestingUserId)
    }

    // 2. READ: Batch Processing
    fun batchValidateAccounts(registrarId: Int, accountIds: List<String>): List<BatchAccountResult> {
        val dataTable = SQLServerDataTable()
        dataTable.addColumnMetadata("AccountId", Types.NVARCHAR)
        for (id in accountIds) {
            dataTable.addRow(id)
        }

        return jdbcTemplate.execute { conn: Connection ->
            val sqlServerConn = conn.unwrap(SQLServerConnection::class.java)
            val call = sqlServerConn.prepareCall("{call usp_BatchValidateAccounts(?, ?)}")
                .unwrap(SQLServerCallableStatement::class.java)

            call.setInt(1, registrarId)
            call.setStructured(2, "dbo.AccountListType", dataTable)

            val rs = call.executeQuery()
            val results = mutableListOf<BatchAccountResult>()

            while (rs.next()) {
                results.add(
                    BatchAccountResult(
                        accountId = rs.getString("AccountId"),
                        globalEntityId = rs.getString("GlobalEntityId"),
                        status = rs.getString("AccountStatus"),
                        riskScore = rs.getInt("RiskScore")
                    )
                )
            }
            return@execute results
        } ?: emptyList()
    }

    // 3. WRITE: Freeze Account
    fun freezeAccount(registrarId: Int, accountId: String, userId: Int, reason: String): String {
        return jdbcTemplate.execute { conn: Connection ->
            val sqlServerConn = conn.unwrap(SQLServerConnection::class.java)
            val call = sqlServerConn.prepareCall("{call usp_FreezeAccount(?, ?, ?, ?)}")

            call.setInt(1, registrarId)
            call.setString(2, accountId)
            call.setInt(3, userId)
            call.setString(4, reason)

            val rs = call.executeQuery()
            if (rs.next()) {
                return@execute rs.getString("NewStatus")
            } else {
                return@execute "FAILED"
            }
        } ?: "ERROR"
    }

    // 4. WRITE: Open Account Simulation
    fun openNewAccount(registrarId: Int, globalEntityId: String, initialBalance: Double): String {
        return jdbcTemplate.execute { conn: Connection ->
            val sqlServerConn = conn.unwrap(SQLServerConnection::class.java)
            val call = sqlServerConn.prepareCall("{call usp_OpenAccount(?, ?, ?, ?, ?)}")

            call.setInt(1, 1) // Requesting User
            call.setString(2, globalEntityId) // Owner Name
            call.setDouble(3, initialBalance) // Initial Balance
            call.setInt(4, registrarId) // Registrar ID
            call.setInt(5, 1) // Participant ID (Default)

            val rs = call.executeQuery()
            if (rs.next()) {
                rs.getString(1)
            } else {
                "ERROR_NO_ID_RETURNED"
            }
        } ?: "ERROR_CONNECTION"
    }
}