package io.github.lukasz756.marspay.earth.outbox;

import java.time.Instant;
import java.util.UUID;

public record PaymentResultPayload(int schemaVersion, UUID paymentId, String status, String reason,
                                   Instant occurredAt) {

    public static PaymentResultPayload create(UUID paymentId, OutboxEventType eventType, String reason) {
        return new PaymentResultPayload(1, paymentId, eventType.resultStatus(), reason, Instant.now());
    }
}