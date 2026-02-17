package com.tsd.platform.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // Allow POST requests without tokens
            .authorizeHttpRequests { it.anyRequest().permitAll() } // Allow EVERYTHING
            .httpBasic { it.disable() }
            .formLogin { it.disable() }

        return http.build()
    }
}