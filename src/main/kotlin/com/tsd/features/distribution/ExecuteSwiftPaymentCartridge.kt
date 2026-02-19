package com.tsd.features.distribution

import com.fasterxml.jackson.databind.ObjectMapper
import com.tsd.adapter.output.persistence.OutboxRepository
import com.tsd.platform.spi.ExchangePacket
import com.tsd.platform.spi.ExecutionContext
import com.tsd.platform.spi.Cartridge
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component("Execute_Swift_Payment")
class ExecuteSwiftPaymentCartridge(
    private val outboxRepository: OutboxRepository,
    private val objectMapper: ObjectMapper
) : Cartridge {

    override val id = "Execute_Swift_Payment"
    override val version = "1.0"
    override val priority = 1

    // 🟢 ADD TRANSACTIONAL HERE! This forces the entire block to be atomic.
    @Transactional
    override fun execute(packet: ExchangePacket, context: ExecutionContext) {
// 🟢 FIXED: Match the JSON keys from your WorkflowRequest
        val amount = packet.data["amount"] ?: "0.00"
        val currency = packet.data["currency"] ?: "THB"

        val jobId = context.getObject<String>("JOB_ID") ?: packet.data["Workflow_ID"] ?: "UNKNOWN-JOB"
        val prefix = context.getObject<String>("STEP_PREFIX") ?: "[N3]"

        println("      ✈️ $prefix Preparing SWIFT MT103 ($currency): $amount")

        val swiftPayload = mapOf(
            "jobId" to jobId.toString(), // 🟢 Safely cast to String here as well just in case
            "amount" to amount.toString(),
            "currency" to currency.toString(),
            "messageType" to "MT103"
        )
        val jsonPayload = objectMapper.writeValueAsString(swiftPayload)

        outboxRepository.saveMessage(
            aggregateType = "SWIFT_PAYMENT",
            aggregateId = jobId.toString(), // 🟢 FIXED: Forcing this to a String
            payload = jsonPayload
        )

        println("         ✅ SWIFT MT103 securely saved to Outbox.")
    }

    override fun compensate(packet: ExchangePacket, context: ExecutionContext) {}
    override fun initialize(context: ExecutionContext) {}
    override fun shutdown() {}
}