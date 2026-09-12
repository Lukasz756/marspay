package io.github.lukasz756.marspay.paymentnode.outbox;

import io.github.lukasz756.marspay.paymentnode.payment.Payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentEventPayload(int schemaVersion, UUID paymentId, UUID sourceBalanceAccountId,
                                  UUID targetBalanceAccountId, long amountMinor, String currency, String reference,
                                  String status, Instant occurredAt) {

    public static PaymentEventPayload from(Payment payment) {
        return new PaymentEventPayload(1, payment.getId(), payment.getSourceBalanceAccountId(),
                                       payment.getTargetBalanceAccountId(), payment.getAmountMinor(),
                                       payment.getCurrency(),
                                       payment.getReference(), payment.getStatus()
                                               .name(), Instant.now());
    }
}