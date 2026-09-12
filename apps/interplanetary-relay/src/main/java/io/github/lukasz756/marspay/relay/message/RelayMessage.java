package io.github.lukasz756.marspay.relay.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "relay_message")
public class RelayMessage {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, updatable = false, length = 50)
    private String source;

    @Column(nullable = false, updatable = false, length = 50)
    private String destination;

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
    private RelayMessageStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

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

    protected RelayMessage() {
    }

    private RelayMessage(UUID eventId, String source, String destination, String aggregateType, UUID aggregateId,
                         String eventType, String payload, Instant availableAt) {
        if (eventId == null) {
            throw new IllegalArgumentException("Relay event id must not be null");
        }

        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Relay message source must not be blank");
        }

        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Relay message destination must not be blank");
        }

        if (aggregateType == null || aggregateType.isBlank()) {
            throw new IllegalArgumentException("Relay aggregate type must not be blank");
        }

        if (aggregateId == null) {
            throw new IllegalArgumentException("Relay aggregate id must not be null");
        }

        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Relay event type must not be blank");
        }

        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("Relay payload must not be blank");
        }

        if (availableAt == null) {
            throw new IllegalArgumentException("Relay available time must not be null");
        }

        this.eventId = eventId;
        this.source = source.trim();
        this.destination = destination.trim();
        this.aggregateType = aggregateType.trim();
        this.aggregateId = aggregateId;
        this.eventType = eventType.trim();
        this.payload = payload;
        this.status = RelayMessageStatus.PENDING;
        this.attemptCount = 0;
        this.availableAt = availableAt;
    }

    public static RelayMessage pending(UUID eventId, String source, String destination, String aggregateType,
                                       UUID aggregateId, String eventType, String payload, Instant availableAt) {
        return new RelayMessage(eventId, source, destination, aggregateType, aggregateId, eventType, payload,
                                availableAt);
    }

    public void markDelivered(Instant deliveredAt) {
        requirePendingStatus();

        if (deliveredAt == null) {
            throw new IllegalArgumentException("Delivered at must not be null");
        }

        this.attemptCount = Math.incrementExact(attemptCount);
        this.status = RelayMessageStatus.DELIVERED;
        this.deliveredAt = deliveredAt;
        this.lastError = null;
    }

    public void recordFailedAttempt(String error, Instant nextAttemptAt, int maxAttempts) {
        requirePendingStatus();

        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("Relay delivery error must not be blank");
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
            this.status = RelayMessageStatus.FAILED;
        } else {
            this.availableAt = nextAttemptAt;
        }
    }

    private void requirePendingStatus() {
        if (status != RelayMessageStatus.PENDING) {
            throw new IllegalStateException("Only pending relay messages can be delivered");
        }
    }


    public UUID getEventId() {
        return eventId;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
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

    public RelayMessageStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getAvailableAt() {
        return availableAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
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
