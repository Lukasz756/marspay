package io.github.lukasz756.marspay.paymentnode.payment;

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
@Table(name = "payment_operation")
public class PaymentOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    private PaymentOperationType type;

    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentOperation() {
    }

    private PaymentOperation(UUID paymentId, PaymentOperationType type, long amountMinor) {
        if (paymentId == null) {
            throw new IllegalArgumentException("Payment id must not be null");
        }

        if (type == null) {
            throw new IllegalArgumentException("Payment operation type must not be null");
        }

        if (amountMinor <= 0) {
            throw new IllegalArgumentException("Payment operation amount must be greater than 0");
        }

        this.paymentId = paymentId;
        this.type = type;
        this.amountMinor = amountMinor;
    }

    public static PaymentOperation record(UUID paymentId, PaymentOperationType type, long amountMinor) {
        return new PaymentOperation(paymentId, type, amountMinor);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public PaymentOperationType getType() {
        return type;
    }

    public long getAmountMinor() {
        return amountMinor;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}