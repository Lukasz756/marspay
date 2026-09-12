package io.github.lukasz756.marspay.paymentnode.ledger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LedgerTransactionResponse(UUID id, UUID paymentId, LedgerTransactionType type, String currency,
                                        String reference, List<LedgerEntryResponse> entries, Instant createdAt) {

    public static LedgerTransactionResponse from(LedgerTransaction transaction, List<LedgerEntry> entries) {
        return new LedgerTransactionResponse(transaction.getId(), transaction.getPaymentId(), transaction.getType(),
                                             transaction.getCurrency(), transaction.getReference(),
                                             entries.stream()
                                                     .map(LedgerEntryResponse::from)
                                                     .toList(), transaction.getCreatedAt());
    }
}