package com.tsd.platform.resilience

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.util.concurrent.TimeUnit

@Component
class IdempotencyKeyInterceptor(
    private val redisTemplate: StringRedisTemplate
) : HandlerInterceptor {

    private val log = LoggerFactory.getLogger(IdempotencyKeyInterceptor::class.java)

    companion object {
        const val IDEMPOTENCY_HEADER = "X-Idempotency-Key"
        const val CACHE_PREFIX = "idempotency:"
        const val TTL_HOURS = 24L
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // 1. Only protect mutating requests (POST, PUT, PATCH, DELETE)
        if (request.method == "GET" || request.method == "OPTIONS") {
            return true
        }

        val idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER)

        // 2. Enforce the Header Contract
        if (idempotencyKey.isNullOrBlank()) {
            log.warn("Rejected mutating request missing $IDEMPOTENCY_HEADER")
            response.status = HttpStatus.BAD_REQUEST.value()
            response.contentType = "application/json"
            response.writer.write("""{"error": "Missing mandatory $IDEMPOTENCY_HEADER header"}""")
            return false
        }

        val cacheKey = "$CACHE_PREFIX$idempotencyKey"

        // 3. The Atomic Lock (Prevents Race Conditions)
        // setIfAbsent returns true ONLY if the key didn't exist before this exact millisecond
        val isNewRequest = redisTemplate.opsForValue().setIfAbsent(cacheKey, "IN_PROGRESS", TTL_HOURS, TimeUnit.HOURS)

        if (isNewRequest == false) {
            val currentState = redisTemplate.opsForValue().get(cacheKey)
            log.warn("Duplicate request intercepted for Key: $idempotencyKey. Current State: $currentState")

            // 4. Neutralize the Duplicate
            response.status = HttpStatus.CONFLICT.value()
            response.contentType = "application/json"
            response.writer.write("""{"error": "Duplicate Request. Transaction is currently $currentState"}""")
            return false
        }

        return true
    }

    override fun afterCompletion(request: HttpServletRequest, response: HttpServletResponse, handler: Any, ex: Exception?) {
        if (request.method == "GET" || request.method == "OPTIONS") return

        val idempotencyKey = request.getHeader(IDEMPOTENCY_HEADER) ?: return
        val cacheKey = "$CACHE_PREFIX$idempotencyKey"

        // 5. Finalize the State
        // If successful, lock the key as COMPLETED for 24 hours.
        // If the server crashed or threw an exception, delete the key so the broker can safely retry.
        if (ex == null && response.status in 200..299) {
            redisTemplate.opsForValue().set(cacheKey, "COMPLETED", TTL_HOURS, TimeUnit.HOURS)
        } else {
            redisTemplate.delete(cacheKey)
        }
    }
}