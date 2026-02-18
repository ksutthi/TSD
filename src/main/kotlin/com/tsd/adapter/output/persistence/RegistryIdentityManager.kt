package com.tsd.adapter.output.persistence

import com.tsd.core.model.LinkResult
import com.tsd.core.port.out.IdentityRepositoryPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.sql.Types
import javax.sql.DataSource

@Component
class RegistryIdentityManager(private val dataSource: DataSource) : IdentityRepositoryPort {

    override suspend fun linkIdentity(
        participantBizId: String,
        localAccountNo: String,
        instrumentBizId: String,
        anchorHash: ByteArray
    ): LinkResult = withContext(Dispatchers.IO) {

        dataSource.connection.use { conn ->
            val sql = "{call dbo.usp_LinkIdentity(?, ?, ?, ?, ?, ?)}"
            conn.prepareCall(sql).use { stmt ->
                stmt.setString(1, participantBizId)
                stmt.setString(2, localAccountNo)
                stmt.setString(3, instrumentBizId)
                stmt.setBytes(4, anchorHash)

                stmt.registerOutParameter(5, Types.NVARCHAR)
                stmt.registerOutParameter(6, Types.NVARCHAR)

                stmt.execute()

                val gin = stmt.getString(5)
                val msg = stmt.getString(6)

                if (msg == "SUCCESS") {
                    LinkResult(true, gin, msg)
                } else {
                    LinkResult(false, null, msg)
                }
            }
        }
    }
}