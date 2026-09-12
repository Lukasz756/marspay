package io.github.lukasz756.marspay.paymentnode.account;

import java.time.Instant;
import java.util.UUID;

public record BalanceAccountResponse(UUID id, UUID accountHolderId, String currency, long availableBalanceMinor,
                                     long reservedBalanceMinor, BalanceAccountStatus status, long version,
                                     Instant createdAt, Instant updatedAt

) {
    public static BalanceAccountResponse from(BalanceAccount balanceAccount) {
        return new BalanceAccountResponse(balanceAccount.getId(), balanceAccount.getAccountHolderId(),
                                          balanceAccount.getCurrency(), balanceAccount.getAvailableBalanceMinor(),
                                          balanceAccount.getReservedBalanceMinor(), balanceAccount.getStatus(),
                                          balanceAccount.getVersion(),
                                          balanceAccount.getCreatedAt(), balanceAccount.getUpdatedAt());
    }
}
