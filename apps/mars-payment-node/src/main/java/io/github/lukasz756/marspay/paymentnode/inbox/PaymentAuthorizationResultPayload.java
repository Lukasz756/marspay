package io.github.lukasz756.marspay.paymentnode.inbox;

import io.github.lukasz756.marspay.paymentnode.payment.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentAuthorizationResultPayload(
        int schemaVersion,
        UUID paymentId,
        PaymentStatus status,
        String reason,
        Instant occurredAt
) {
}