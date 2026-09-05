package io.github.lukasz756.marspay.paymentnode.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository
        extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findAllByLedgerTransactionIdOrderByCreatedAtAsc(
            UUID ledgerTransactionId
    );

    List<LedgerEntry> findAllByBalanceAccountIdOrderByCreatedAtAsc(
            UUID balanceAccountId
    );
}