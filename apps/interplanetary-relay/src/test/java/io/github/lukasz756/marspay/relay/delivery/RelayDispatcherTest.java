package io.github.lukasz756.marspay.relay.delivery;

import io.github.lukasz756.marspay.relay.message.RelayDeliveryMessage;
import io.github.lukasz756.marspay.relay.message.RelayMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.TEMPORAL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelayDispatcherTest {

    @Mock
    private RelayMessageService relayMessageService;

    @Mock
    private RelayTransport relayTransport;

    private RelayDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new RelayDispatcher(
                relayMessageService,
                relayTransport
        );
    }

    @Test
    void shouldDeliverReadyMessageAndMarkItAsDelivered() {
        var eventId = UUID.randomUUID();

        var message = new RelayDeliveryMessage(
                eventId,
                "MARS_PAYMENT_NODE",
                "EARTH_PAYMENT_SERVICE",
                "PAYMENT",
                UUID.randomUUID(),
                "PAYMENT_CREATED",
                """
                {
                  "schemaVersion": 1
                }
                """,
                Instant.now()
        );

        when(relayMessageService.findReadyMessages(any(Instant.class)))
                .thenReturn(List.of(message));

        Instant beforeDispatch = Instant.now();

        dispatcher.dispatchReadyMessages();

        Instant afterDispatch = Instant.now();

        verify(relayTransport).deliver(message);

        var deliveredAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(relayMessageService).markDelivered(
                eq(eventId),
                deliveredAtCaptor.capture()
        );

        assertThat(deliveredAtCaptor.getValue())
                .isBetween(beforeDispatch, afterDispatch);

        verify(relayMessageService, never()).recordFailedAttempt(
                any(),
                any(),
                any(),
                any(Integer.class)
        );
    }

    @Test
    void recordsFailedAttemptWhenDeliveryFails() {
        var eventId = UUID.randomUUID();

        var message = new RelayDeliveryMessage(
                eventId,
                "MARS_PAYMENT_NODE",
                "EARTH_PAYMENT_SERVICE",
                "PAYMENT",
                UUID.randomUUID(),
                "PAYMENT_CREATED",
                """
                {
                  "schemaVersion": 1
                }
                """,
                Instant.now()
        );

        when(relayMessageService.findReadyMessages(any(Instant.class)))
                .thenReturn(List.of(message));

        doThrow(new RuntimeException("connection timeout"))
                .when(relayTransport)
                .deliver(message);

        var beforeDispatch = Instant.now();

        dispatcher.dispatchReadyMessages();

        var afterDispatch = Instant.now();

        verify(relayTransport).deliver(message);

        var nextAttemptAtCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(relayMessageService).recordFailedAttempt(
                eq(eventId),
                eq("RuntimeException: connection timeout"),
                nextAttemptAtCaptor.capture(),
                eq(5)
        );

        assertThat(nextAttemptAtCaptor.getValue())
                .isBetween(
                        beforeDispatch.plusSeconds(30),
                        afterDispatch.plusSeconds(30)
                );

        verify(relayMessageService, never()).markDelivered(
                any(),
                any()
        );
    }
}