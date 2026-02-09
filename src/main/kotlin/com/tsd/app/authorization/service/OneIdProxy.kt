package com.tsd.app.authorization.service

import com.tsd.app.authorization.model.OneIdRequest
import com.tsd.app.authorization.model.OneIdResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class OneIdProxy(
    private val restTemplate: RestTemplate,
    // 👇 CHANGE THIS LINE from 8090 to 8080
    @Value("\${integration.one-id.url:http://localhost:8080/api/auth/verify}")
     // 🟢 Configurable with default
    private val oneIdUrl: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OneIdProxy::class.java)
    }

    fun checkAccess(user: String): Boolean {
        logger.info("📞 [TSD Registry] Calling SET One ID for user: {}...", user)

        // --- 🟢 TESTING BACKDOOR ---
        if (user.equals("admin", ignoreCase = true) || user.equals("system", ignoreCase = true)) {
            logger.warn("✅ [TSD Registry] TEST MODE: Granting automatic access to '{}'", user)
            return true
        }

        return try {
            val request = OneIdRequest(userId = user)

            val response = restTemplate.postForObject(
                oneIdUrl,
                request,
                OneIdResponse::class.java
            )

            if (response != null && response.allowed) {
                logger.info("✅ [TSD Registry] One ID Approved! (Risk: {})", response.riskScore)
                true
            } else {
                logger.warn("⛔ [TSD Registry] One ID Denied: {}", response?.reason)
                false
            }

        } catch (e: Exception) {
            logger.error("💥 [TSD Registry] Connection Failed: {}", e.message)
            false
        }
    }
}