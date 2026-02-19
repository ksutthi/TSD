package com.tsd.adapter.output.persistence

// 🟢 Explicit import pointing to where jOOQ generated the class
import com.tsd.adapter.out.persistence.jooq.schema.tables.OutboxMessages
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Repository
class OutboxRepository(private val dsl: DSLContext) {

    @Transactional(propagation = Propagation.REQUIRED)
    fun saveMessage(aggregateType: String, aggregateId: String, payload: String) {
        dsl.insertInto(OutboxMessages.OUTBOX_MESSAGES)
            .set(OutboxMessages.OUTBOX_MESSAGES.MESSAGE_ID, UUID.randomUUID())
            .set(OutboxMessages.OUTBOX_MESSAGES.AGGREGATE_TYPE, aggregateType)
            .set(OutboxMessages.OUTBOX_MESSAGES.AGGREGATE_ID, aggregateId)
            .set(OutboxMessages.OUTBOX_MESSAGES.PAYLOAD, payload)
            .set(OutboxMessages.OUTBOX_MESSAGES.STATUS, "PENDING")
            .set(OutboxMessages.OUTBOX_MESSAGES.CREATED_AT, LocalDateTime.now())
            .execute()
    }

    fun fetchPendingMessages() = dsl.selectFrom(OutboxMessages.OUTBOX_MESSAGES)
        .where(OutboxMessages.OUTBOX_MESSAGES.STATUS.eq("PENDING"))
        .fetch()

    fun markAsProcessed(messageId: UUID) {
        dsl.update(OutboxMessages.OUTBOX_MESSAGES)
            .set(OutboxMessages.OUTBOX_MESSAGES.STATUS, "COMPLETED")
            .set(OutboxMessages.OUTBOX_MESSAGES.PROCESSED_AT, LocalDateTime.now())
            .where(OutboxMessages.OUTBOX_MESSAGES.MESSAGE_ID.eq(messageId))
            .execute()
    }
} // 🟢 Make sure this brace is here!