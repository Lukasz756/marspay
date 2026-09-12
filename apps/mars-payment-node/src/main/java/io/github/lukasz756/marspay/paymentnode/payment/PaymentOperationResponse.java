package io.github.lukasz756.marspay.paymentnode.payment;

import java.time.Instant;
import java.util.UUID;

public record PaymentOperationResponse(UUID id, UUID paymentId, PaymentOperationType type, long amountMinor,
                                       Instant createdAt) {

    public static PaymentOperationResponse from(PaymentOperation operation) {
        return new PaymentOperationResponse(operation.getId(), operation.getPaymentId(), operation.getType(),
                                            operation.getAmountMinor(), operation.getCreatedAt());
    }
}