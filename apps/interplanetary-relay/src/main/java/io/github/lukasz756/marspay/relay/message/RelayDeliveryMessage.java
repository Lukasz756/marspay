package io.github.lukasz756.marspay.relay.message;

import java.time.Instant;
import java.util.UUID;

public record RelayDeliveryMessage(UUID eventId, String source, String destination, String aggregateType,
                                   UUID aggregateId, String eventType, String payloadJson, Instant receivedAt) {

    public static RelayDeliveryMessage from(RelayMessage message) {
        return new RelayDeliveryMessage(message.getEventId(), message.getSource(), message.getDestination(),
                                        message.getAggregateType(), message.getAggregateId(), message.getEventType(),
                                        message.getPayload(),
                                        message.getReceivedAt());
    }
}