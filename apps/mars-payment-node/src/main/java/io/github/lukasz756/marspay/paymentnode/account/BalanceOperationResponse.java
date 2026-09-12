package io.github.lukasz756.marspay.paymentnode.account;

import java.time.Instant;
import java.util.UUID;

public record BalanceOperationResponse(UUID id, UUID balanceAccountId, BalanceOperationType type, long amountMinor,
                                       String reference, long availableBalanceAfterMinor,
                                       long reservedBalanceAfterMinor, Instant createdAt, UUID transferId) {
    public static BalanceOperationResponse from(BalanceOperation operation) {
        return new BalanceOperationResponse(operation.getId(), operation.getBalanceAccountId(), operation.getType(),
                                            operation.getAmountMinor(), operation.getReference(),
                                            operation.getAvailableBalanceAfterMinor(),
                                            operation.getReservedBalanceAfterMinor(), operation.getCreatedAt(),
                                            operation.getTransferId());
    }
}
