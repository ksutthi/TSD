package com.tsd.app

import com.tsd.app.functional_area.account.service.HoldersProcessor
import com.tsd.app.functional_area.authorization.service.RegistryIdentityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional
import javax.sql.DataSource

@SpringBootTest
class HoldersProcessorTest {

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var identityManager: RegistryIdentityManager

    @Test
    @Transactional
    @Commit // THIS IS KEY: It forces Spring to save the data to SQL after the test
    fun testProcessRealCsv() {
        val processor = HoldersProcessor(dataSource, identityManager)

        // Ensure this path matches where your file actually sits
        val path = "data/registry/holders.csv"

        println("--- STARTING CSV INGESTION ---")
        processor.processHoldersCsv(path)
        println("--- INGESTION FINISHED ---")
    }
}