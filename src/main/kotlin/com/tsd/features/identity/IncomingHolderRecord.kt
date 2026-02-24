package com.tsd.features.identity

/**
 * The Input payload representing a raw, unstructured row from a Book Closing file
 * or the Registration_Queue.
 */
data class IncomingHolderRecord(
    val queueId: Int,
    val firstName: String,
    val lastName: String,
    val taxId: String?,
    val idType: String, // e.g., "CITIZEN_ID", "PASSPORT"
    val idValue: String,
    val country: String
)