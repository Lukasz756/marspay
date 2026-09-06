package io.github.lukasz756.marspay.paymentnode.outbox;

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
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "aggregate_type",
            nullable = false,
            updatable = false,
            length = 50
    )
    private OutboxAggregateType aggregateType;

    @Column(
            name = "aggregate_id",
            nullable = false,
            updatable = false
    )
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "event_type",
            nullable = false,
            updatable = false,
            length = 50
    )
    private OutboxEventType eventType;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OutboxEvent() {
    }

    private OutboxEvent(
            OutboxAggregateType aggregateType,
            UUID aggregateId,
            OutboxEventType eventType,
            String payload
    ) {
        if (aggregateType == null) {
            throw new IllegalArgumentException(
                    "Outbox aggregate type must not be null"
            );
        }

        if (aggregateId == null) {
            throw new IllegalArgumentException(
                    "Outbox aggregate id must not be null"
            );
        }

        if (eventType == null) {
            throw new IllegalArgumentException(
                    "Outbox event type must not be null"
            );
        }

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(
                    "Outbox payload must not be blank"
            );
        }

        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = OutboxEventStatus.PENDING;
        this.attemptCount = 0;
        this.availableAt = Instant.now();
    }

    public static OutboxEvent pendingPayment(
            UUID paymentId,
            OutboxEventType eventType,
            String payload
    ) {
        return new OutboxEvent(
                OutboxAggregateType.PAYMENT,
                paymentId,
                eventType,
                payload
        );
    }

    public void markPublished(Instant publishedAt) {
        requirePendingStatus();

        if (publishedAt == null) {
            throw new IllegalArgumentException(
                    "Published at must not be null"
            );
        }

        this.attemptCount = Math.incrementExact(attemptCount);
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void recordFailedAttempt(
            String error,
            Instant nextAttemptAt,
            int maxAttempts
    ) {
        requirePendingStatus();

        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException(
                    "Outbox error must not be blank"
            );
        }

        if (nextAttemptAt == null) {
            throw new IllegalArgumentException(
                    "Next attempt time must not be null"
            );
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException(
                    "Maximum attempts must be greater than 0"
            );
        }

        this.attemptCount = Math.incrementExact(attemptCount);
        this.lastError = error.trim();

        if (attemptCount >= maxAttempts) {
            this.status = OutboxEventStatus.FAILED;
        } else {
            this.availableAt = nextAttemptAt;
        }
    }

    private void requirePendingStatus() {
        if (status != OutboxEventStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending outbox events can be processed"
            );
        }
    }

    public UUID getId() {
        return id;
    }

    public OutboxAggregateType getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getLastError() {
        return lastError;
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