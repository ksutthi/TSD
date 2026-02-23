package com.tsd.core.service

import org.springframework.stereotype.Service
import com.tsd.core.port.output.IdentityRepositoryPort
import com.tsd.core.model.LinkResult

@Service
class IdentityService(
    // The Core asks for the Interface (Port), not the Class (Adapter)
    private val identityRepository: IdentityRepositoryPort
) {

    suspend fun linkUser(
        participantId: String,
        accountNo: String,
        instrumentId: String,
        hash: ByteArray
    ): LinkResult {
        // Business Logic happens here (e.g., validation)
        if (participantId.isBlank()) throw IllegalArgumentException("Invalid ID")

        // Call the port (Coroutines propagate automatically)
        return identityRepository.linkIdentity(
            participantId,
            accountNo,
            instrumentId,
            hash
        )
    }
}