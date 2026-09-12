package io.github.lukasz756.marspay.relay.message;

import io.github.lukasz756.marspay.relay.delivery.RelayProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RelayMessageService {

    private final RelayMessageRepository relayMessageRepository;
    private final ObjectMapper objectMapper;
    private final RelayProperties relayProperties;

    RelayMessageService(RelayMessageRepository relayMessageRepository, ObjectMapper objectMapper,
                        RelayProperties relayProperties) {
        this.relayMessageRepository = relayMessageRepository;
        this.objectMapper = objectMapper;
        this.relayProperties = relayProperties;
    }

    @Transactional
    public RelayReceiveResult receive(RelayMessageRequest request) {
        if (relayMessageRepository.existsById(request.eventId())) {
            return new RelayReceiveResult(request.eventId(), true);
        }

        try {
            String payloadJson = objectMapper.writeValueAsString(request.payload());

            Instant availableAt = Instant.now()
                    .plus(relayProperties.deliveryDelay());

            RelayMessage message = RelayMessage.pending(request.eventId(), request.source(), request.destination(),
                                                        request.aggregateType(), request.aggregateId(),
                                                        request.eventType(), payloadJson, availableAt);

            relayMessageRepository.saveAndFlush(message);

            return new RelayReceiveResult(message.getEventId(), false);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Cannot serialize relay message payload", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<RelayDeliveryMessage> findReadyMessages(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("Current time must not be null");
        }

        return relayMessageRepository.findTop50ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscReceivedAtAsc(
                        RelayMessageStatus.PENDING, now)
                .stream()
                .map(RelayDeliveryMessage::from)
                .toList();
    }

    @Transactional
    public void markDelivered(UUID eventId, Instant deliveredAt) {
        RelayMessage message = findMessage(eventId);
        message.markDelivered(deliveredAt);
    }

    @Transactional
    public void recordFailedAttempt(UUID eventId, String error, Instant nextAttemptAt, int maxAttempts) {
        RelayMessage message = findMessage(eventId);

        message.recordFailedAttempt(error, nextAttemptAt, maxAttempts);
    }

    private RelayMessage findMessage(UUID eventId) {
        return relayMessageRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Relay message " +
                                                                     "not found: " + eventId));
    }
}