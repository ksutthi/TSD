package com.tsd.core.model

// 📦 DATA STRUCTURES
// This file just holds the "Shape" of the data we send to the UI.

data class Snapshot(
    val id: SnapshotId,
    val accountNumber: String, // Custody Account Number
    val name: String,
    val investorName: String,
    val quantity: Int,
    val lastUpdated: String
)