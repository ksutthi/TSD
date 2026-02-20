package com.tsd.core.model

import java.math.BigDecimal

/**
 * The pure domain output payload.
 * Types now perfectly match the database (Int for Instrument, BigDecimal for Quantity).
 */
data class AggregatedPosition(
    val instrumentId: Int,
    val totalQuantity: BigDecimal
)