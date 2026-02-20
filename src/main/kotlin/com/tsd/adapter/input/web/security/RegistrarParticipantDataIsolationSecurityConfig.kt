package com.tsd.adapter.input.web.security

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
open class RegistrarParticipantDataIsolationSecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    @Value("\${tsd.security.registrar-and-participant-data-isolation.enabled:false}")
    private val isIsolationEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    // ANSI Color Codes for the console
    private val ANSI_RED = "\u001B[31m"
    private val ANSI_GREEN = "\u001B[32m"
    private val ANSI_RESET = "\u001B[0m"

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        if (!isIsolationEnabled) {
            // 🔴 LOUD RED BANNER WHEN SECURITY IS OFF
            log.warn("""
                $ANSI_RED
                ======================================================================
                ⚠️ WARNING: REGISTRAR AND PARTICIPANT DATA ISOLATION IS DISABLED!
                🚪 Bypassing all JWT security checks for local testing.
                🛑 DO NOT DEPLOY THIS TO PRODUCTION!
                ======================================================================$ANSI_RESET
            """.trimIndent())

            http.authorizeHttpRequests { it.anyRequest().permitAll() }
        } else {
            // 🟢 GREEN TEXT WHEN SECURITY IS ON
            log.info("$ANSI_GREEN🛡️ REGISTRAR AND PARTICIPANT DATA ISOLATION IS ENABLED. Enforcing strict JWT Role-Based Access Control.$ANSI_RESET")

            http.authorizeHttpRequests {
                // Open the door for login
                it.requestMatchers("/api/v1/auth/**").permitAll()

                // Protect our endpoints based on Roles
                it.requestMatchers("/api/v1/portfolio/god-view").hasRole("TSD_ADMIN")
                it.requestMatchers("/api/v1/portfolio/isolated-view").hasAnyRole("BROKER", "REGISTRAR")

                it.anyRequest().authenticated()
            }

            // Inject our Token Filter
            http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        }

        return http.build()
    }
}