package io.github.lukasz756.marspay.paymentnode.inbox;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inbox_event")
public class InboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false, length = 50)
    private String source;

    @Column(name = "aggregate_type", nullable = false, updatable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private String eventType;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InboxEvent() {
    }

    private InboxEvent(UUID eventId, String source, String aggregateType, UUID aggregateId, String eventType,
                       String payload) {
        if (eventId == null) {
            throw new IllegalArgumentException("Inbox event id must not be null");
        }

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Inbox event source must not be blank");
        }

        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("Inbox aggregate type must not be blank");
        }

        if (aggregateId == null) {
            throw new IllegalArgumentException("Inbox aggregate id must not be null");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Inbox event type must not be blank");
        }

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Inbox payload must not be blank");
        }

        this.eventId = eventId;
        this.source = source.trim();
        this.aggregateType = aggregateType.trim();
        this.aggregateId = aggregateId;
        this.eventType = eventType.trim();
        this.payload = payload;
        this.status = InboxEventStatus.PENDING;
        this.attemptCount = 0;
        this.availableAt = Instant.now();
    }

    public static InboxEvent pending(UUID eventId, String source, String aggregateType, UUID aggregateId,
                                     String eventType, String payload) {
        return new InboxEvent(eventId, source, aggregateType, aggregateId, eventType, payload);
    }

    public void markProcessed(Instant processedAt) {
        requirePendingStatus();

        if (processedAt == null) {
            throw new IllegalArgumentException("Processed at must not be null");
        }

        this.attemptCount = Math.incrementExact(attemptCount);
        this.status = InboxEventStatus.PROCESSED;
        this.processedAt = processedAt;
        this.lastError = null;
    }

    public void recordFailedAttempt(String error, Instant nextAttemptAt, int maxAttempts) {
        requirePendingStatus();

        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("Inbox error must not be blank");
        }

        if (nextAttemptAt == null) {
            throw new IllegalArgumentException("Next attempt time must not be null");
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Maximum attempts must be greater than 0");
        }

        this.attemptCount = Math.incrementExact(attemptCount);
        this.lastError = error.trim();

        if (attemptCount >= maxAttempts) {
            this.status = InboxEventStatus.FAILED;
        } else {
            this.availableAt = nextAttemptAt;
        }
    }

    private void requirePendingStatus() {
        if (status != InboxEventStatus.PENDING) {
            throw new IllegalStateException("Only pending inbox events can be processed");
        }
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getSource() {
        return source;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public InboxEventStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public long getVersion() {
        return version;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}