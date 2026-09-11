package io.github.lukasz756.marspay.earth.inbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InboxService {

    private final InboxEventRepository inboxEventRepository;
    private final ObjectMapper objectMapper;
    private final PaymentInboxEventHandler paymentInboxEventHandler;

    InboxService(
            InboxEventRepository inboxEventRepository,
            ObjectMapper objectMapper, PaymentInboxEventHandler paymentInboxEventHandler
    ) {
        this.inboxEventRepository = inboxEventRepository;
        this.objectMapper = objectMapper;
        this.paymentInboxEventHandler = paymentInboxEventHandler;
    }

    @Transactional
    public InboxReceiveResult receive(IncomingEventRequest request) {
        if (inboxEventRepository.existsById(request.eventId())) {
            return new InboxReceiveResult(
                    request.eventId(),
                    true
            );
        }

        try {
            String payloadJson =
                    objectMapper.writeValueAsString(request.payload());

            InboxEvent event = InboxEvent.pending(
                    request.eventId(),
                    request.source(),
                    request.aggregateType(),
                    request.aggregateId(),
                    request.eventType(),
                    payloadJson
            );

            inboxEventRepository.saveAndFlush(event);

            return new InboxReceiveResult(
                    event.getEventId(),
                    false
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Cannot serialize inbox event payload",
                    exception
            );
        }
    }
    @Transactional(readOnly = true)
    public List<UUID> findReadyEventIds(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "Current time must not be null"
            );
        }

        return inboxEventRepository
                .findTop50ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscReceivedAtAsc(
                        InboxEventStatus.PENDING,
                        now
                )
                .stream()
                .map(InboxEvent::getEventId)
                .toList();
    }

    @Transactional
    public void processEvent(
            UUID eventId,
            Instant processedAt
    ) {
        if (processedAt == null) {
            throw new IllegalArgumentException(
                    "Processed at must not be null"
            );
        }

        InboxEvent event = findEvent(eventId);

        if (event.getStatus() != InboxEventStatus.PENDING) {
            return;
        }

        paymentInboxEventHandler.handle(event);
        event.markProcessed(processedAt);
    }

    @Transactional
    public void recordFailedAttempt(
            UUID eventId,
            String error,
            Instant nextAttemptAt,
            int maxAttempts
    ) {
        InboxEvent event = findEvent(eventId);

        event.recordFailedAttempt(
                error,
                nextAttemptAt,
                maxAttempts
        );
    }

    private InboxEvent findEvent(UUID eventId) {
        return inboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Inbox event not found: " + eventId
                ));
    }
}