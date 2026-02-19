package com.tsd.platform.engine.relay

// 🟢 Explicit import pointing to where jOOQ generated the class
import com.tsd.adapter.out.persistence.jooq.schema.tables.OutboxMessages
import com.tsd.adapter.output.persistence.OutboxRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OutboxRelayWorker(private val outboxRepository: OutboxRepository) {

    @Scheduled(fixedDelay = 5000)
    fun processOutbox() {
        val pendingMessages = outboxRepository.fetchPendingMessages()

        if (pendingMessages.isNotEmpty) {
            println("📮 [OutboxRelay] Found ${pendingMessages.size} pending messages to send...")
        }

        for (record in pendingMessages) {
            val messageId = record[OutboxMessages.OUTBOX_MESSAGES.MESSAGE_ID] as UUID
            val payload = record[OutboxMessages.OUTBOX_MESSAGES.PAYLOAD] as String
            val type = record[OutboxMessages.OUTBOX_MESSAGES.AGGREGATE_TYPE] as String

            try {
                sendToExternalSystem(type, payload)
                outboxRepository.markAsProcessed(messageId)
                println("   ✅ [OutboxRelay] Successfully sent Message $messageId")
            } catch (e: Exception) {
                println("   ❌ [OutboxRelay] Failed to send Message $messageId. Will retry. Error: ${e.message}")
            }
        }
    }

    private fun sendToExternalSystem(type: String, payload: String) {
        // 🔴 INJECT NETWORK FAILURE HERE:
        // throw RuntimeException("HTTP 503: SWIFT Network Gateway is offline!")
         Thread.sleep(200)
    }
}