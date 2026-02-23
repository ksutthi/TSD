package com.tsd.app

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

// Injecting the overrides directly here preserves your database connections
// but forces Hibernate to build the Maker-Checker tables for the test.
@SpringBootTest(properties = [
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.flyway.enabled=false"
])
class TsdApplicationTests {

    @Test
    fun contextLoads() {
    }
}
