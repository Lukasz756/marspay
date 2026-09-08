package io.github.lukasz756.marspay.paymentnode.inbox;

import java.time.Instant;
import java.util.UUID;

public record PaymentResultPayload(
        int schemaVersion,
        UUID paymentId,
        String status,
        String reason,
        Instant occurredAt
) {
}