package com.tsd.adapter.input.security

import com.tsd.core.model.UserIdentity
import com.tsd.core.port.output.SecurityContextPort
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.math.BigDecimal

@Component
@Profile("local", "test")
class MockSecurityFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(MockSecurityFilter::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val mockUser = request.getHeader("X-Mock-User")

        if (mockUser != null) {
            val role = request.getHeader("X-Mock-Role") ?: "USER"
            val broker = request.getHeader("X-Mock-Broker") ?: "TSD_INTERNAL"
            val limit = request.getHeader("X-Mock-Limit") ?: "0.00"
            val fullName = request.getHeader("X-Mock-FullName") ?: "Mock Identity"

            val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
            val authToken = UsernamePasswordAuthenticationToken(mockUser, null, authorities)

            // Store our custom JPA attributes in Spring's generic details map
            authToken.details = mapOf(
                "brokerCode" to broker,
                "approvalLimit" to limit,
                "fullName" to fullName
            )

            SecurityContextHolder.getContext().authentication = authToken
            log.info("🛡️ [SECURITY MOCK] Identity Injected -> User: $mockUser | Role: $role | Limit: $limit")
        }
        filterChain.doFilter(request, response)
    }
}

@Component
class SpringSecurityContextAdapter : SecurityContextPort {
    override fun getCurrentUser(): UserIdentity {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authentication found in security context")

        val details = auth.details as? Map<*, *> ?: emptyMap<Any, Any>()

        // Extract the primary role from Spring's GrantedAuthority
        val primaryRole = auth.authorities.firstOrNull()?.authority?.removePrefix("ROLE_") ?: "USER"

        // Map Spring context directly to your JPA Entity / Domain Model
        return UserIdentity(
            userId = auth.name,
            fullName = details["fullName"]?.toString() ?: "Mock Identity",
            role = primaryRole,
            brokerCode = details["brokerCode"]?.toString() ?: "UNKNOWN",
            approvalLimit = BigDecimal(details["approvalLimit"]?.toString() ?: "0.00")
        )
    }
}