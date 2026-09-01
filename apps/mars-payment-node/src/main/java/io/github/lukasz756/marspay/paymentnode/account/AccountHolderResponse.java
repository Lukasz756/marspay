package io.github.lukasz756.marspay.paymentnode.account;

import java.time.Instant;
import java.util.UUID;

public record AccountHolderResponse(
        UUID id,
        String reference,
        AccountHolderType type,
        AccountHolderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static AccountHolderResponse from(AccountHolder accountHolder) {
        return new AccountHolderResponse(
                accountHolder.getId(),
                accountHolder.getReference(),
                accountHolder.getType(),
                accountHolder.getStatus(),
                accountHolder.getCreatedAt(),
                accountHolder.getUpdatedAt()
        );
    }
}