package io.github.lukasz756.marspay.paymentnode.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID sourceBalanceAccountId,
        UUID targetBalanceAccountId,
        long amountMinor,
        String currency,
        String reference,
        PaymentStatus status,
        Instant createdAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getSourceBalanceAccountId(),
                payment.getTargetBalanceAccountId(),
                payment.getAmountMinor(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}