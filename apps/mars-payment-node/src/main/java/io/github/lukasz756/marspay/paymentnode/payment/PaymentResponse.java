package io.github.lukasz756.marspay.paymentnode.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(UUID id, UUID sourceBalanceAccountId, UUID targetBalanceAccountId, long amountMinor,
                              String currency, String reference, PaymentStatus status, String processingReason,
                              Instant createdAt, Instant updatedAt) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getSourceBalanceAccountId(),
                                   payment.getTargetBalanceAccountId(), payment.getAmountMinor(), payment.getCurrency(),
                                   payment.getReference(), payment.getStatus(), payment.getProcessingReason(),
                                   payment.getCreatedAt(), payment.getUpdatedAt());
    }
}
