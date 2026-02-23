package com.tsd.adapter.output.connectivity

import com.tsd.platform.engine.util.EngineAnsi
import com.tsd.platform.spi.UniversalHttpPort // 🟢 Import the Port
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class UniversalHttpAdapter : UniversalHttpPort { // 🟢 Implement the Port!

    private val restClient = RestClient.create()

    @CircuitBreaker(name = "universal-api", fallbackMethod = "fallbackCall")
    @Retry(name = "universal-api")
    override fun executeCall(traceId: String, url: String, method: String, payload: Map<String, Any>): Boolean {
        println("      🌐 [UniversalHttp] Initiating $method to $url (Trace: $traceId)...")

        val request = when (method.uppercase()) {
            "POST" -> restClient.post().uri(url).body(payload)
            "PUT" -> restClient.put().uri(url).body(payload)
            "GET" -> restClient.get().uri(url)
            else -> throw IllegalArgumentException("Unsupported HTTP Method: $method")
        }

        val response = request
            .header("X-Trace-Id", traceId)
            .retrieve()
            .toBodilessEntity()

        if (response.statusCode.is2xxSuccessful) {
            println(EngineAnsi.GREEN + "      ✅ [UniversalHttp] API Call Successful! (200 OK)" + EngineAnsi.RESET)
            return true
        } else {
            throw RuntimeException("External API returned error status: ${response.statusCode}")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun fallbackCall(traceId: String, url: String, method: String, payload: Map<String, Any>, ex: Exception): Boolean {
        println(EngineAnsi.RED + "      ❌ [CircuitBreaker] External API is DOWN! URL: $url | Reason: ${ex.message}" + EngineAnsi.RESET)
        throw RuntimeException("Universal API Gateway Failure: ${ex.message}")
    }
}