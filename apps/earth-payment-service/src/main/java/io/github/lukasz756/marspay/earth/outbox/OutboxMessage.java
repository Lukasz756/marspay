package io.github.lukasz756.marspay.earth.outbox;

import java.time.Instant;
import java.util.UUID;

public record OutboxMessage(
        UUID eventId,
        OutboxAggregateType aggregateType,
        UUID aggregateId,
        OutboxEventType eventType,
        String payloadJson,
        Instant createdAt
) {

    public static OutboxMessage from(OutboxEvent event) {
        return new OutboxMessage(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getEventType(),
                event.getPayload(),
                event.getCreatedAt()
        );
    }
}