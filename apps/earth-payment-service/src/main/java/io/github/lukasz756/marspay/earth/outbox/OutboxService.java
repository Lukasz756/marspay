package io.github.lukasz756.marspay.earth.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    OutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordPaymentResult(
            UUID paymentId,
            OutboxEventType eventType,
            String reason
    ) {
        if (paymentId == null) {
            throw new IllegalArgumentException(
                    "Payment id must not be null"
            );
        }

        if (eventType == null) {
            throw new IllegalArgumentException(
                    "Outbox event type must not be null"
            );
        }

        PaymentResultPayload payload =
                PaymentResultPayload.create(
                        paymentId,
                        eventType,
                        reason
                );

        try {
            String payloadJson =
                    objectMapper.writeValueAsString(payload);

            OutboxEvent event = OutboxEvent.pendingPayment(
                    paymentId,
                    eventType,
                    payloadJson
            );

            outboxEventRepository.save(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Cannot serialize payment result event",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public List<OutboxMessage> findReadyEvents(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "Current time must not be null"
            );
        }

        return outboxEventRepository
                .findTop50ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscCreatedAtAsc(
                        OutboxEventStatus.PENDING,
                        now
                )
                .stream()
                .map(OutboxMessage::from)
                .toList();
    }

    @Transactional
    public void markPublished(
            UUID eventId,
            Instant publishedAt
    ) {
        OutboxEvent event = findEvent(eventId);
        event.markPublished(publishedAt);
    }

    @Transactional
    public void recordFailedAttempt(
            UUID eventId,
            String error,
            Instant nextAttemptAt,
            int maxAttempts
    ) {
        OutboxEvent event = findEvent(eventId);

        event.recordFailedAttempt(
                error,
                nextAttemptAt,
                maxAttempts
        );
    }

    private OutboxEvent findEvent(UUID eventId) {
        return outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event not found: " + eventId
                ));
    }
}