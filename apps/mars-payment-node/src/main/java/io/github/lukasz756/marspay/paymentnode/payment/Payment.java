package io.github.lukasz756.marspay.paymentnode.payment;


import io.github.lukasz756.marspay.paymentnode.payment.exceptions.PaymentInvalidStatusException;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.PaymentSameAccountException;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "payment")
public class Payment {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_balance_account_id", nullable = false, updatable = false)
    private UUID sourceBalanceAccountId;

    @Column(name = "target_balance_account_id", nullable = false, updatable = false)
    private UUID targetBalanceAccountId;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "reference", nullable = false, updatable = false, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {
    }

    private Payment(
            UUID sourceBalanceAccountId,
            UUID targetBalanceAccountId,
            long amountMinor,
            String currency,
            String reference
    ) {
        if (sourceBalanceAccountId == null || targetBalanceAccountId == null) {
            throw new IllegalArgumentException(
                    "Source and target balance account ids must not be null"
            );
        }

        if (sourceBalanceAccountId.equals(targetBalanceAccountId)) {
            throw new PaymentSameAccountException(sourceBalanceAccountId);
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than 0"
            );
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Currency must not be blank"
            );
        }

        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(
                    "Currency must contain exactly three letters"
            );
        }

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference must not be blank"
            );
        }

        String normalizedReference = reference.trim();

        if (normalizedReference.length() > 100) {
            throw new IllegalArgumentException(
                    "Payment reference must be at most 100 characters"
            );
        }

        this.sourceBalanceAccountId = sourceBalanceAccountId;
        this.targetBalanceAccountId = targetBalanceAccountId;
        this.amountMinor = amountMinor;
        this.currency = normalizedCurrency;
        this.reference = normalizedReference;
        this.status = PaymentStatus.CREATED;
    }

    public static Payment create(UUID sourceBalanceAccountId, UUID targetBalanceAccountId, long amountMinor, String currency, String reference) {
        return new Payment(
                sourceBalanceAccountId,
                targetBalanceAccountId,
                amountMinor,
                currency,
                reference
        );
    }

    public void authorize() {
        if (status != PaymentStatus.CREATED) {
            throw new PaymentInvalidStatusException(
                    id,
                    status,
                    PaymentStatus.CREATED
            );
        }

        status = PaymentStatus.AUTHORIZED;
    }

    public void capture() {
        if (status != PaymentStatus.AUTHORIZED) {
            throw new PaymentInvalidStatusException(
                    id,
                    status,
                    PaymentStatus.AUTHORIZED
            );
        }

        status = PaymentStatus.CAPTURED;
    }

    public void cancel() {
        if (status != PaymentStatus.CREATED
                && status != PaymentStatus.AUTHORIZED) {
            throw new PaymentInvalidStatusException(
                    id,
                    status,
                    PaymentStatus.CREATED,
                    PaymentStatus.AUTHORIZED
            );
        }

        status = PaymentStatus.CANCELLED;
    }

    public void refund() {
        if (status != PaymentStatus.CAPTURED) {
            throw new PaymentInvalidStatusException(
                    id,
                    status,
                    PaymentStatus.CAPTURED
            );
        }

        status = PaymentStatus.REFUNDED;
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

    public PaymentStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
