package io.github.lukasz756.marspay.paymentnode.payment;


import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "balance_transfer")
public class BalanceTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
            name = "source_balance_account_id",
            nullable = false,
            updatable = false
    )
    private UUID sourceBalanceAccountId;

    @Column(
            name = "target_balance_account_id",
            nullable = false,
            updatable = false
    )
    private UUID targetBalanceAccountId;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(nullable = false, updatable = false, length = 100)
    private String reference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BalanceTransfer() {

    }

    private BalanceTransfer(UUID sourceBalanceAccountId, UUID targetBalanceAccountId, long amountMinor, String currency, String reference) {
        if (sourceBalanceAccountId == null || targetBalanceAccountId == null) {
            throw new IllegalArgumentException("Source/target balance account cannot be null");
        }

        if (sourceBalanceAccountId.equals(targetBalanceAccountId)) {
            throw new IllegalArgumentException(
                    "Target balance account cannot be the same as source"
            );
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Transfer reference must not be blank"
            );
        }

        String normalizedReference = reference.trim();

        if (normalizedReference.length() > 100) {
            throw new IllegalArgumentException(
                    "Transfer reference must be at most 100 characters"
            );
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }

        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must contain exactly three letters");
        }

        this.sourceBalanceAccountId = sourceBalanceAccountId;
        this.targetBalanceAccountId = targetBalanceAccountId;
        this.amountMinor = amountMinor;
        this.reference = normalizedReference;
        this.currency = normalizedCurrency;

    }

    public static BalanceTransfer create(
            UUID sourceBalanceAccountId,
            UUID targetBalanceAccountId,
            long amountMinor,
            String currency,
            String reference
    ) {
        return new BalanceTransfer(sourceBalanceAccountId, targetBalanceAccountId, amountMinor, currency, reference);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSourceBalanceAccountId() {
        return sourceBalanceAccountId;
    }

    public UUID getTargetBalanceAccountId() {
        return targetBalanceAccountId;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getCurrency() {
        return currency;
    }

    public String getReference() {
        return reference;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
