package com.tsd.platform.config

import com.tsd.platform.resilience.IdempotencyKeyInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val idempotencyKeyInterceptor: IdempotencyKeyInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        // This forces every API request to pass through the Idempotency Shield
        registry.addInterceptor(idempotencyKeyInterceptor)
            .addPathPatterns("/api/v1/**")
    }
}