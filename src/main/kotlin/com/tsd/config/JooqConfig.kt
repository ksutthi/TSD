package com.tsd.config

import org.jooq.conf.RenderNameCase
import org.jooq.conf.Settings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JooqConfig {

    @Bean
    fun jooqSettings(): org.jooq.conf.Settings {
        return Settings()
            .withRenderSchema(false)       // 👈 The Magic Line: Hides "PUBLIC"
            .withRenderNameCase(RenderNameCase.AS_IS)
    }
}