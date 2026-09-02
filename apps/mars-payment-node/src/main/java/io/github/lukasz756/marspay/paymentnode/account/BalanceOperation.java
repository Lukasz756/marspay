package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "balance_operation")
public class BalanceOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "balance_account_id", nullable = false, updatable = false)
    private UUID balanceAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private BalanceOperationType type;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(nullable = false, updatable = false, length = 100)
    private String reference;

    @Column(name = "available_balance_after_minor", nullable = false, updatable = false)
    private long availableBalanceAfterMinor;

    @Column(name = "reserved_balance_after_minor", nullable = false, updatable = false)
    private long reservedBalanceAfterMinor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BalanceOperation() {

    }

    private BalanceOperation(UUID balanceAccountId, long amountMinor, String reference, long availableBalanceAfterMinor, long reservedBalanceAfterMinor) {
        if (balanceAccountId == null) {
            throw new IllegalArgumentException("Balance account id must not be null");
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Operation's amount must be greater than 0");
        }

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Reference must not be blank"
            );
        }

        String normalizedReference = reference.trim();

        if (normalizedReference.length() > 100) {
            throw new IllegalArgumentException(
                    "Reference must be at most 100 characters"
            );
        }

        if (availableBalanceAfterMinor < 0) {
            throw new IllegalArgumentException(
                    "Available balance after operation must not be negative"
            );
        }

        if (reservedBalanceAfterMinor < 0) {
            throw new IllegalArgumentException(
                    "Reserved balance after operation must not be negative"
            );
        }

        this.balanceAccountId = balanceAccountId;
        this.amountMinor = amountMinor;
        this.reference = normalizedReference;
        this.availableBalanceAfterMinor = availableBalanceAfterMinor;
        this.reservedBalanceAfterMinor = reservedBalanceAfterMinor;
        this.type = BalanceOperationType.CREDIT;

    }

    public static BalanceOperation credit(
            UUID balanceAccountId,
            long amountMinor,
            String reference,
            long availableBalanceAfterMinor,
            long reservedBalanceAfterMinor
    ) {
        return new BalanceOperation(
                balanceAccountId,
                amountMinor,
                reference,
                availableBalanceAfterMinor,
                reservedBalanceAfterMinor
        );
    }

    public UUID getId() {
        return id;
    }

    public UUID getBalanceAccountId() {
        return balanceAccountId;
    }

    public BalanceOperationType getType() {
        return type;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public String getReference() {
        return reference;
    }

    public long getAvailableBalanceAfterMinor() {
        return availableBalanceAfterMinor;
    }

    public long getReservedBalanceAfterMinor() {
        return reservedBalanceAfterMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}