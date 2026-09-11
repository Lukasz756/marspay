package io.github.lukasz756.marspay.earth.inbox;

import java.time.Instant;
import java.util.UUID;

public record PaymentCommandPayload(
        int schemaVersion,
        UUID paymentId,
        UUID sourceBalanceAccountId,
        UUID targetBalanceAccountId,
        long amountMinor,
        String currency,
        String reference,
        String status,
        Instant occurredAt
) {
}