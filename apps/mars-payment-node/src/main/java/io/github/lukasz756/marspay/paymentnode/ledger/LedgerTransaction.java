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
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "ledger_transaction")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private LedgerTransactionType type;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(nullable = false, updatable = false, length = 100)
    private String reference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    private LedgerTransaction(UUID paymentId, LedgerTransactionType type, String currency, String reference) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment id must not be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("Ledger transaction type must not be null");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Ledger transaction currency must not be blank");
        }

        String normalizedCurrency = currency.trim()
                .toUpperCase(Locale.ROOT);

        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Ledger transaction currency must contain " + "exactly three letters");
        }

        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Ledger transaction reference must not be blank");
        }

        String normalizedReference = reference.trim();

        if (normalizedReference.length() > 100) {
            throw new IllegalArgumentException("Ledger transaction reference must be " + "at most 100 characters");
        }

        this.paymentId = paymentId;
        this.type = type;
        this.currency = normalizedCurrency;
        this.reference = normalizedReference;
    }

    public static LedgerTransaction forPayment(UUID paymentId, LedgerTransactionType type, String currency,
                                               String reference) {
        return new LedgerTransaction(paymentId, type, currency, reference);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public LedgerTransactionType getType() {
        return type;
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