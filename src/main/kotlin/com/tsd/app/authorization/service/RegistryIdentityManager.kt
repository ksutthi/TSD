package com.tsd.app.authorization.service

import org.springframework.stereotype.Component
import java.sql.Types
import javax.sql.DataSource

@Component // This tells Spring "I am a bean, you can inject me!"
class RegistryIdentityManager(private val dataSource: DataSource) {

    data class LinkResult(val success: Boolean, val gin: String?, val message: String)
    fun linkIdentity(
        participantBizId: String,
        localAccountNo: String,
        instrumentBizId: String,
        anchorHash: ByteArray
    ): LinkResult {
        dataSource.connection.use { conn ->
            val sql = "{call dbo.usp_LinkIdentity(?, ?, ?, ?, ?, ?)}"
            conn.prepareCall(sql).use { stmt ->
                stmt.setString(1, participantBizId)
                stmt.setString(2, localAccountNo)
                stmt.setString(3, instrumentBizId)
                stmt.setBytes(4, anchorHash)

                stmt.registerOutParameter(5, Types.NVARCHAR) // @GIN
                stmt.registerOutParameter(6, Types.NVARCHAR) // @Message

                stmt.execute()

                val gin = stmt.getString(5)
                val msg = stmt.getString(6)

                return if (msg == "SUCCESS") {
                    LinkResult(true, gin, msg)
                } else {
                    LinkResult(false, null, msg)
                }
            }
        }
    }
}