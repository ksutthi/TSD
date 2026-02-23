package com.tsd.platform.exception

import io.micrometer.tracing.Tracer
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE) // This tells Spring: "Use this interceptor BEFORE any others"
class PlatformExceptionHandler(
    private val tracer: Tracer // <-- Inject tracer here
    ) { // Renamed to avoid the bean collision


    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request parameters")
        problem.title = "Bad Request"
        problem.type = URI.create("https://api.tsd.com/errors/bad-request")
        return problem
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.message ?: "State transition conflict")
        problem.title = "Business Rule Violation"
        problem.type = URI.create("https://api.tsd.com/errors/conflict")
        return problem
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleOptimisticLocking(ex: ObjectOptimisticLockingFailureException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The record was updated by another transaction. Please refresh and try again.")
        problem.title = "Concurrency Conflict"
        problem.type = URI.create("https://api.tsd.com/errors/concurrency-conflict")
        return problem
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected system error occurred.")
        problem.title = "Internal Server Error"
        problem.type = URI.create("https://api.tsd.com/errors/internal-error")

        // Dynamically add the Trace ID to the JSON payload!
        val traceId = tracer.currentTraceContext().context()?.traceId() ?: "unknown"
        problem.setProperty("traceId", traceId)

        return problem
    }
}