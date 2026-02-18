package com.tsd.adapter.input.web

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {

        // 🟢 CLEAN LOG: Only prints the error message (No massive stack trace)
        logger.error("💥 [API-ERROR] {} | Path: {}", ex.message, request.requestURI)

        // 🔍 DEBUG: Keeps the stack trace available if you ever need to dig deeper
        logger.debug("Stack Trace Details:", ex)

        val errorResponse = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "status"    to "ERROR",
            "error"     to (ex::class.simpleName ?: "UnknownError"),
            "message"   to (ex.message ?: "An unexpected error occurred"),
            "path"      to request.requestURI
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse)
    }
}