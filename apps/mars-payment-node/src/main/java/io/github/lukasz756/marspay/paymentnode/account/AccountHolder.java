package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "account_holder")
public class AccountHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private AccountHolderType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountHolderStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountHolder() {
    }


    private AccountHolder(String reference, AccountHolderType type) {
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("Account holder reference must not be blank");
        }

        String normalizedReference = reference.trim();

        if (normalizedReference.length() > 100) {
            throw new IllegalArgumentException("Account holder reference must be at most 100 characters");
        }

        this.reference = normalizedReference;
        this.type = Objects.requireNonNull(type, "Account holder type must not be null");
        this.status = AccountHolderStatus.ACTIVE;
    }

    public static AccountHolder create(String reference, AccountHolderType type) {
        return new AccountHolder(reference, type);
    }

    public UUID getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public AccountHolderType getType() {
        return type;
    }

    public AccountHolderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}