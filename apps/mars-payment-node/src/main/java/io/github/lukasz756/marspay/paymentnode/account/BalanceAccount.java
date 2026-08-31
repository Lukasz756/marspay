package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "balance_account")
public class BalanceAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_holder_id", nullable = false, updatable = false)
    private UUID accountHolderId;

    @Column(nullable = false, updatable = false, length = 3)
    private String currency;

    @Column(name = "available_balance_minor", nullable = false)
    private long availableBalanceMinor;

    @Column(name = "reserved_balance_minor", nullable = false)
    private long reservedBalanceMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BalanceAccountStatus status;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BalanceAccount() {
    }

    private BalanceAccount(UUID accountHolderId, String currency) {
        if (accountHolderId == null) {
            throw new IllegalArgumentException("Account holder id must not be null");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }

        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

        if (!normalizedCurrency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must contain exactly three letters");
        }

        this.accountHolderId = accountHolderId;
        this.currency = normalizedCurrency;
        this.availableBalanceMinor = 0;
        this.reservedBalanceMinor = 0;
        this.status = BalanceAccountStatus.ACTIVE;
    }

    public static BalanceAccount open(UUID accountHolderId, String currency) {
        return new BalanceAccount(accountHolderId, currency);
    }

    public UUID getId() {
        return id;
    }

    public UUID getAccountHolderId() {
        return accountHolderId;
    }

    public String getCurrency() {
        return currency;
    }

    public long getAvailableBalanceMinor() {
        return availableBalanceMinor;
    }

    public long getReservedBalanceMinor() {
        return reservedBalanceMinor;
    }

    public BalanceAccountStatus getStatus() {
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