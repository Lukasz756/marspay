package io.github.lukasz756.marspay.paymentnode.ledger;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "ledger_transaction_id",
            nullable = false,
            updatable = false
    )
    private UUID ledgerTransactionId;

    @Column(
            name = "balance_account_id",
            nullable = false,
            updatable = false
    )
    private UUID balanceAccountId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "balance_bucket",
            nullable = false,
            updatable = false,
            length = 20
    )
    private LedgerBalanceBucket balanceBucket;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
    }

    private LedgerEntry(
            UUID ledgerTransactionId,
            UUID balanceAccountId,
            LedgerBalanceBucket balanceBucket,
            long amountMinor
    ) {
        if (ledgerTransactionId == null) {
            throw new IllegalArgumentException(
                    "Ledger transaction id must not be null"
            );
        }

        if (balanceAccountId == null) {
            throw new IllegalArgumentException(
                    "Balance account id must not be null"
            );
        }

        if (balanceBucket == null) {
            throw new IllegalArgumentException(
                    "Ledger balance bucket must not be null"
            );
        }

        if (amountMinor == 0) {
            throw new IllegalArgumentException(
                    "Ledger entry amount must not be 0"
            );
        }

        this.ledgerTransactionId = ledgerTransactionId;
        this.balanceAccountId = balanceAccountId;
        this.balanceBucket = balanceBucket;
        this.amountMinor = amountMinor;
    }

    public static LedgerEntry decrease(
            UUID ledgerTransactionId,
            UUID balanceAccountId,
            LedgerBalanceBucket balanceBucket,
            long amountMinor
    ) {
        requirePositiveAmount(amountMinor);

        return new LedgerEntry(
                ledgerTransactionId,
                balanceAccountId,
                balanceBucket,
                -amountMinor
        );
    }

    public static LedgerEntry increase(
            UUID ledgerTransactionId,
            UUID balanceAccountId,
            LedgerBalanceBucket balanceBucket,
            long amountMinor
    ) {
        requirePositiveAmount(amountMinor);

        return new LedgerEntry(
                ledgerTransactionId,
                balanceAccountId,
                balanceBucket,
                amountMinor
        );
    }

    private static void requirePositiveAmount(long amountMinor) {
        if (amountMinor <= 0) {
            throw new IllegalArgumentException(
                    "Ledger entry amount must be greater than 0"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public UUID getBalanceAccountId() {
        return balanceAccountId;
    }

    public LedgerBalanceBucket getBalanceBucket() {
        return balanceBucket;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}