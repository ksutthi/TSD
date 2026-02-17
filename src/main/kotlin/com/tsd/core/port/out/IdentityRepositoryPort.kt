package com.tsd.core.port.out

import com.tsd.core.model.LinkResult

interface IdentityRepositoryPort {
    suspend fun linkIdentity(
        participantBizId: String,
        localAccountNo: String,
        instrumentBizId: String,
        anchorHash: ByteArray
    ): LinkResult
}