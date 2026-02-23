package com.tsd.core.service

import com.tsd.core.port.output.IdentityRepositoryPort
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.math.BigDecimal
import javax.sql.DataSource

@Service
class HoldersProcessor(
    private val dataSource: DataSource,
    // Dependency Injection: Asking for the Interface (Port), not the Class
    private val identityRepository: IdentityRepositoryPort
) {

    @Transactional
    fun processHoldersCsv(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            println("ERROR: File NOT FOUND at ${file.absolutePath}")
            return
        }

        file.bufferedReader().useLines { lines ->
            lines.drop(1).forEach { line ->
                val cols = line.split(",").map { it.trim() }

                // Expecting 7 columns
                if (cols.size >= 7) {
                    try {
                        val pId = cols[0]
                        val accNo = cols[1]
                        val insId = cols[2]
                        val name = cols[3]
                        val addr = cols[4]
                        val hash = hexStringToByteArray(cols[5]) // Helper function at bottom
                        val balance = cols[6].toBigDecimal()

                        // 1. IDENTITY LINKING
                        // Since 'linkIdentity' is a suspend function (Coroutine),
                        // we must wrap it in 'runBlocking' to call it from this normal function.
                        val link = runBlocking {
                            identityRepository.linkIdentity(pId, accNo, insId, hash)
                        }

                        if (link.success && link.gin != null) {
                            // 2. SAVE ATTRIBUTES
                            saveAttributes(link.gin, pId, accNo, name, addr)
                            // 3. UPDATE POSITION
                            upsertPosition(link.gin, pId, accNo, insId, balance)

                            println("PROCESSED: GIN ${link.gin} | Acc $accNo | Balance $balance")
                        }
                    } catch (e: Exception) {
                        println("ERROR on line: $line -> ${e.message}")
                    }
                }
            }
        }
    }

    private fun saveAttributes(gin: String, pId: String, accNo: String, name: String, addr: String) {
        dataSource.connection.use { conn ->
            val sql = """
                INSERT INTO dbo.Identity_Attributes (GIN, Member_Internal_ID, Local_Account_No, Attribute_Type, Attribute_Value)
                VALUES (?, (SELECT Member_Internal_ID FROM dbo.Member_Master WHERE Member_Business_ID = ?), ?, 'NAME_TH', ?),
                       (?, (SELECT Member_Internal_ID FROM dbo.Member_Master WHERE Member_Business_ID = ?), ?, 'ADDRESS', ?)
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, gin); stmt.setString(2, pId); stmt.setString(3, accNo); stmt.setString(4, name)
                stmt.setString(5, gin); stmt.setString(6, pId); stmt.setString(7, accNo); stmt.setString(8, addr)
                stmt.executeUpdate()
            }
        }
    }

    private fun upsertPosition(gin: String, pId: String, accNo: String, insId: String, balance: BigDecimal) {
        dataSource.connection.use { conn ->
            val sql = """
                MERGE dbo.Position_Balances AS target
                USING (SELECT ? AS GIN, 
                              (SELECT Member_Internal_ID FROM dbo.Member_Master WHERE Member_Business_ID = ?) AS MID,
                              ? AS Acc,
                              (SELECT Instrument_Internal_ID FROM dbo.Instrument_Master WHERE Instrument_Business_ID = ?) AS IID) AS source
                ON (target.Member_Internal_ID = source.MID AND target.Local_Account_No = source.Acc AND target.Instrument_Internal_ID = source.IID)
                WHEN MATCHED THEN
                    UPDATE SET Balance_Quantity = ?, Available_Quantity = ?, Last_Updated_Date = GETDATE()
                WHEN NOT MATCHED THEN
                    INSERT (GIN, Member_Internal_ID, Local_Account_No, Instrument_Internal_ID, Balance_Quantity, Available_Quantity)
                    VALUES (source.GIN, source.MID, source.Acc, source.IID, ?, ?);
            """.trimIndent()

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, gin); stmt.setString(2, pId); stmt.setString(3, accNo); stmt.setString(4, insId)
                stmt.setBigDecimal(5, balance); stmt.setBigDecimal(6, balance)
                stmt.setBigDecimal(7, balance); stmt.setBigDecimal(8, balance)
                stmt.executeUpdate()
            }
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val data = ByteArray(s.length / 2)
        for (i in 0 until s.length step 2) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
        }
        return data
    }
}