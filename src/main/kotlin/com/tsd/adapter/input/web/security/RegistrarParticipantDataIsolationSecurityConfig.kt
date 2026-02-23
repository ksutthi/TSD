package com.tsd.adapter.input.web.security

import com.tsd.adapter.input.security.MockSecurityFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

    // 1. Safely inject our Mock Filter. It only exists when the 'local' profile is active!
    @Autowired(required = false) private val mockSecurityFilter: MockSecurityFilter?,

    @Value("\${tsd.security.registrar-and-participant-data-isolation.enabled:false}")
    private val isIsolationEnabled: Boolean
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    private val ANSI_RED = "\u001B[31m"
    private val ANSI_GREEN = "\u001B[32m"
    private val ANSI_BLUE = "\u001B[34m"
    private val ANSI_RESET = "\u001B[0m"

    @Bean
    open fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        if (!isIsolationEnabled) {
            log.warn("""
                $ANSI_RED
                ======================================================================
                ⚠️ WARNING: REGISTRAR AND PARTICIPANT DATA ISOLATION IS DISABLED!
                🚪 Bypassing all JWT security checks for local testing.
                🛑 DO NOT DEPLOY THIS TO PRODUCTION!
                ======================================================================$ANSI_RESET
            """.trimIndent())

            http.authorizeHttpRequests { it.anyRequest().permitAll() }

            // 2. INJECT THE MOCK SHIELD HERE!
            // If we are in local dev, attach the header interceptor.
            if (mockSecurityFilter != null) {
                log.info("$ANSI_BLUE🛡️ LOCAL MOCK SECURITY FILTER ACTIVATED. Ready to accept X-Mock-* headers.$ANSI_RESET")
                http.addFilterBefore(mockSecurityFilter, UsernamePasswordAuthenticationFilter::class.java)
            }

        } else {
            log.info("$ANSI_GREEN🛡️ REGISTRAR AND PARTICIPANT DATA ISOLATION IS ENABLED. Enforcing strict JWT Role-Based Access Control.$ANSI_RESET")

            http.authorizeHttpRequests {
                it.requestMatchers("/api/v1/auth/**").permitAll()
                it.requestMatchers("/api/v1/portfolio/god-view").hasRole("TSD_ADMIN")
                it.requestMatchers("/api/v1/portfolio/isolated-view").hasAnyRole("BROKER", "REGISTRAR")
                it.anyRequest().authenticated()
            }

            http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
        }

        return http.build()
    }
}