package com.tsd.platform.event

import org.springframework.context.ApplicationEvent
import java.math.BigDecimal

/**
 * A System Event triggered when a payment is fully authorized and processed.
 * Used to decouple the PDF Generator from the Notification System.
 */
class PaymentCompletedEvent(
    source: Any,
    val accountId: Long,
    val amount: BigDecimal
) : ApplicationEvent(source)