package io.github.lukasz756.marspay.relay.message;

import io.github.lukasz756.marspay.relay.delivery.RelayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelayMessageServiceTest {

    private static final Duration DELIVERY_DELAY =
            Duration.ofSeconds(30);

    @Mock
    private RelayMessageRepository relayMessageRepository;

    private ObjectMapper objectMapper;
    private RelayMessageService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        RelayProperties relayProperties = new RelayProperties(
                DELIVERY_DELAY,
                List.of(
                        new RelayProperties.Destination(
                                "EARTH_PAYMENT_SERVICE",
                                URI.create(
                                        "http://earth-payment-service.test/internal/relay/events"
                                )
                        )
                )
        );

        service = new RelayMessageService(
                relayMessageRepository,
                objectMapper,
                relayProperties
        );
    }

    @Test
    void savesNewMessageWithDeliveryDelay() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        RelayMessageRequest request = relayRequest(
                eventId,
                aggregateId
        );

        when(relayMessageRepository.existsById(eventId))
                .thenReturn(false);

        Instant beforeReceive = Instant.now();

        RelayReceiveResult result = service.receive(
                "MARS_PAYMENT_NODE",
                "EARTH_PAYMENT_SERVICE",
                request
        );

        Instant afterReceive = Instant.now();

        ArgumentCaptor<RelayMessage> messageCaptor =
                ArgumentCaptor.forClass(RelayMessage.class);

        verify(relayMessageRepository)
                .saveAndFlush(messageCaptor.capture());

        RelayMessage savedMessage = messageCaptor.getValue();

        assertThat(result.eventId()).isEqualTo(eventId);
        assertThat(result.duplicate()).isFalse();

        assertThat(savedMessage.getEventId()).isEqualTo(eventId);
        assertThat(savedMessage.getAggregateId()).isEqualTo(aggregateId);
        assertThat(savedMessage.getSource())
                .isEqualTo("MARS_PAYMENT_NODE");
        assertThat(savedMessage.getDestination())
                .isEqualTo("EARTH_PAYMENT_SERVICE");
        assertThat(savedMessage.getAggregateType())
                .isEqualTo("PAYMENT");
        assertThat(savedMessage.getEventType())
                .isEqualTo("PAYMENT_CREATED");
        assertThat(savedMessage.getStatus())
                .isEqualTo(RelayMessageStatus.PENDING);
        assertThat(savedMessage.getAttemptCount()).isZero();

        assertThat(objectMapper.readTree(savedMessage.getPayload()))
                .isEqualTo(request.payload());

        assertThat(savedMessage.getAvailableAt())
                .isBetween(
                        beforeReceive.plus(DELIVERY_DELAY),
                        afterReceive.plus(DELIVERY_DELAY)
                );
    }

    @Test
    void returnsDuplicateResultWithoutSavingExistingMessage()
            throws Exception {
        UUID eventId = UUID.randomUUID();

        RelayMessageRequest request = relayRequest(
                eventId,
                UUID.randomUUID()
        );

        when(relayMessageRepository.existsById(eventId))
                .thenReturn(true);

        RelayReceiveResult result = service.receive(
                "MARS_PAYMENT_NODE",
                "EARTH_PAYMENT_SERVICE",
                request
        );

        assertThat(result.eventId()).isEqualTo(eventId);
        assertThat(result.duplicate()).isTrue();

        verify(relayMessageRepository, never())
                .saveAndFlush(any(RelayMessage.class));
    }

    @Test
    void returnsMessagesReadyForDelivery() {
        Instant now = Instant.parse("2035-01-10T12:00:00Z");
        RelayMessage message = pendingMessage(
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        when(relayMessageRepository
                     .findTop50ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscReceivedAtAsc(
                             RelayMessageStatus.PENDING,
                             now
                     ))
                .thenReturn(List.of(message));

        List<RelayDeliveryMessage> result =
                service.findReadyMessages(now);

        assertThat(result)
                .singleElement()
                .satisfies(deliveryMessage -> {
                    assertThat(deliveryMessage.eventId())
                            .isEqualTo(message.getEventId());

                    assertThat(deliveryMessage.source())
                            .isEqualTo(message.getSource());

                    assertThat(deliveryMessage.destination())
                            .isEqualTo(message.getDestination());

                    assertThat(deliveryMessage.aggregateId())
                            .isEqualTo(message.getAggregateId());

                    assertThat(deliveryMessage.eventType())
                            .isEqualTo(message.getEventType());

                    assertThat(deliveryMessage.payloadJson())
                            .isEqualTo(message.getPayload());
                });
    }

    @Test
    void marksStoredMessageAsDelivered() {
        UUID eventId = UUID.randomUUID();
        RelayMessage message = pendingMessage(
                eventId,
                UUID.randomUUID()
        );

        Instant deliveredAt =
                Instant.parse("2035-01-10T12:01:00Z");

        when(relayMessageRepository.findById(eventId))
                .thenReturn(Optional.of(message));

        service.markDelivered(eventId, deliveredAt);

        assertThat(message.getStatus())
                .isEqualTo(RelayMessageStatus.DELIVERED);

        assertThat(message.getDeliveredAt())
                .isEqualTo(deliveredAt);

        assertThat(message.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void recordsFailedAttemptForStoredMessage() {
        UUID eventId = UUID.randomUUID();
        RelayMessage message = pendingMessage(
                eventId,
                UUID.randomUUID()
        );

        Instant nextAttemptAt =
                Instant.parse("2035-01-10T12:00:30Z");

        when(relayMessageRepository.findById(eventId))
                .thenReturn(Optional.of(message));

        service.recordFailedAttempt(
                eventId,
                "connection timeout",
                nextAttemptAt,
                5
        );

        assertThat(message.getStatus())
                .isEqualTo(RelayMessageStatus.PENDING);

        assertThat(message.getAttemptCount()).isEqualTo(1);
        assertThat(message.getLastError())
                .isEqualTo("connection timeout");

        assertThat(message.getAvailableAt())
                .isEqualTo(nextAttemptAt);
    }

    private RelayMessageRequest relayRequest(
            UUID eventId,
            UUID aggregateId
    ) throws Exception {
        return new RelayMessageRequest(
                eventId,
                "PAYMENT",
                aggregateId,
                "PAYMENT_CREATED",
                objectMapper.readTree("""
                                              {
                                                "schemaVersion": 1,
                                                "status": "CREATED"
                                              }
                                              """)
        );
    }

    private RelayMessage pendingMessage(
            UUID eventId,
            UUID aggregateId
    ) {
        return RelayMessage.pending(
                eventId,
                "MARS_PAYMENT_NODE",
                "EARTH_PAYMENT_SERVICE",
                "PAYMENT",
                aggregateId,
                "PAYMENT_CREATED",
                """
                        {
                          "schemaVersion": 1,
                          "status": "CREATED"
                        }
                        """,
                Instant.parse("2035-01-10T12:00:00Z")
        );
    }
}