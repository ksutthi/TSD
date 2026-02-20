package com.tsd.core.model

/**
 * The flexible input command for the core domain.
 * The investor can provide EITHER the GIN_ID directly, OR their Broker Account details.
 */
data class GodViewQuery(
    val ginId: Long? = null,
    val participantId: String? = null,
    val accountId: String? = null
)