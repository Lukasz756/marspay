package io.github.lukasz756.marspay.paymentnode.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface LedgerTransactionRepository
        extends JpaRepository<LedgerTransaction, UUID> {

    List<LedgerTransaction> findAllByPaymentIdOrderByCreatedAtAsc(
            UUID paymentId
    );
}