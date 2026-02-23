package com.tsd.platform.config

import io.micrometer.tracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TraceIdResponseFilter(
    private val tracer: Tracer
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Grab the current trace context
        val traceContext = tracer.currentTraceContext().context()

        if (traceContext != null) {
            // Attach it to the HTTP Response header
            response.setHeader("X-Trace-Id", traceContext.traceId())
        }

        filterChain.doFilter(request, response)
    }
}