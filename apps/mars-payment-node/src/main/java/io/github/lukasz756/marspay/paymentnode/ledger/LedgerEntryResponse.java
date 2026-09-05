package io.github.lukasz756.marspay.paymentnode.ledger;

import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID balanceAccountId,
        LedgerBalanceBucket balanceBucket,
        long amountMinor,
        Instant createdAt
) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getBalanceAccountId(),
                entry.getBalanceBucket(),
                entry.getAmountMinor(),
                entry.getCreatedAt()
        );
    }
}