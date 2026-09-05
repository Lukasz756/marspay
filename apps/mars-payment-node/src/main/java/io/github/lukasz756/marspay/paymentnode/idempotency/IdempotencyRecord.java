package io.github.lukasz756.marspay.paymentnode.idempotency;

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
import java.util.UUID;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 50)
    private IdempotencyScope scope;

    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false,
            length = 255
    )
    private String idempotencyKey;

    @Column(
            name = "request_hash",
            nullable = false,
            updatable = false,
            length = 64
    )
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_location", length = 255)
    private String responseLocation;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdempotencyRecord() {
    }

    private IdempotencyRecord(
            IdempotencyScope scope,
            String idempotencyKey,
            String requestHash
    ) {
        if (scope == null) {
            throw new IllegalArgumentException(
                    "Idempotency scope must not be null"
            );
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency key must not be blank"
            );
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.length() > 255) {
            throw new IllegalArgumentException(
                    "Idempotency key must be at most 255 characters"
            );
        }

        if (requestHash == null
                || !requestHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Request hash must be a lowercase SHA-256 hash"
            );
        }

        this.scope = scope;
        this.idempotencyKey = normalizedKey;
        this.requestHash = requestHash;
        this.status = IdempotencyStatus.IN_PROGRESS;
    }

    public static IdempotencyRecord start(
            IdempotencyScope scope,
            String idempotencyKey,
            String requestHash
    ) {
        return new IdempotencyRecord(
                scope,
                idempotencyKey,
                requestHash
        );
    }

    public void complete(
            int responseStatus,
            String responseBody,
            String responseLocation
    ) {
        if (status != IdempotencyStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Idempotency record is already completed"
            );
        }

        if (responseStatus < 100 || responseStatus > 599) {
            throw new IllegalArgumentException(
                    "Invalid HTTP response status"
            );
        }

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalArgumentException(
                    "Response body must not be blank"
            );
        }

        if (responseLocation != null
                && responseLocation.length() > 255) {
            throw new IllegalArgumentException(
                    "Response location must be at most 255 characters"
            );
        }

        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.responseLocation = responseLocation;
        this.status = IdempotencyStatus.COMPLETED;
    }

    public UUID getId() {
        return id;
    }

    public IdempotencyScope getScope() {
        return scope;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getResponseLocation() {
        return responseLocation;
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