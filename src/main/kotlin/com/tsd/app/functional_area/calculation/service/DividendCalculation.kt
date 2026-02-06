package com.tsd.app.functional_area.calculation.service

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class DividendCalculation {

    fun calculateGross(quantity: BigDecimal, rate: BigDecimal): BigDecimal {
        return quantity.multiply(rate).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateTax(grossAmount: BigDecimal, taxRate: BigDecimal): BigDecimal {
        return grossAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP)
    }

    fun calculateNet(grossAmount: BigDecimal, taxAmount: BigDecimal): BigDecimal {
        return grossAmount.subtract(taxAmount)
    }

    // Helper to safely convert any object to BigDecimal (used by cartridges)
    fun safeBigDecimal(value: Any?): BigDecimal {
        return when (value) {
            is BigDecimal -> value
            is Double -> BigDecimal.valueOf(value)
            is Int -> BigDecimal.valueOf(value.toLong())
            is String -> try { BigDecimal(value) } catch (e: Exception) { BigDecimal.ZERO }
            else -> BigDecimal.ZERO
        }
    }
}