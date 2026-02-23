package com.tsd.platform.config

import com.tsd.platform.resilience.IdempotencyKeyInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val idempotencyKeyInterceptor: IdempotencyKeyInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(idempotencyKeyInterceptor)
            // Apply this strictly to your core business APIs
            .addPathPatterns("/api/v1/corporate-actions/**")
            .addPathPatterns("/api/v1/ledger/**")
            // Exclude public or read-only paths if necessary
            .excludePathPatterns("/api/v1/health")
    }
}