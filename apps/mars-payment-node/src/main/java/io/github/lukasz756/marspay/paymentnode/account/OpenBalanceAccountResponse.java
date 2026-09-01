package io.github.lukasz756.marspay.paymentnode.account;

import java.time.Instant;
import java.util.UUID;

public record OpenBalanceAccountResponse(
        UUID id,
        UUID accountHolderId,
        String currency,
        long availableBalanceMinor,
        long reservedBalanceMinor,
        BalanceAccountStatus status,
        long version,
        Instant createdAt,
        Instant updatedAt

) {
    public static OpenBalanceAccountResponse from(BalanceAccount balanceAccount) {
        return new OpenBalanceAccountResponse(
                balanceAccount.getId(),
                balanceAccount.getAccountHolderId(),
                balanceAccount.getCurrency(),
                balanceAccount.getAvailableBalanceMinor(),
                balanceAccount.getReservedBalanceMinor(),
                balanceAccount.getStatus(),
                balanceAccount.getVersion(),
                balanceAccount.getCreatedAt(),
                balanceAccount.getUpdatedAt()
        );
    }
}
