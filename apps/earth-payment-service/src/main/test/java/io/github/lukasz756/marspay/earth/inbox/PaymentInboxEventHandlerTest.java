package io.github.lukasz756.marspay.earth.inbox;

import io.github.lukasz756.marspay.earth.outbox.OutboxEventType;
import io.github.lukasz756.marspay.earth.outbox.OutboxService;
import io.github.lukasz756.marspay.earth.simulation.PaymentSimulationPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentInboxEventHandlerTest {

    private static final String SOURCE = "MARS_PAYMENT_NODE";
    private static final String AGGREGATE_TYPE = "PAYMENT";

    @Mock
    private PaymentSimulationPolicy simulationPolicy;

    @Mock
    private OutboxService outboxService;

    private ObjectMapper objectMapper;
    private PaymentInboxEventHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new PaymentInboxEventHandler(
                objectMapper,
                simulationPolicy,
                outboxService
        );
    }

    @Test
    void shouldProcessAuthorizationRequestAndRecordResult() throws Exception {
        var payload = paymentPayload("AUTHORIZATION_PENDING");
        var event = inboxEvent(
                payload,
                SOURCE,
                AGGREGATE_TYPE,
                "PAYMENT_AUTHORIZATION_REQUESTED"
        );

        var decision = new PaymentSimulationPolicy.Decision(
                OutboxEventType.PAYMENT_AUTHORIZED,
                null
        );

        when(simulationPolicy.decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                0
        )).thenReturn(decision);

        handler.handle(event);

        verify(simulationPolicy).decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                0
        );

        verify(outboxService).recordPaymentResult(
                payload.paymentId(),
                OutboxEventType.PAYMENT_AUTHORIZED,
                null
        );
    }

    @Test
    void shouldNotProduceResultForPaymentCreatedEvent() throws Exception {
        var payload = paymentPayload("CREATED");
        var event = inboxEvent(
                payload,
                SOURCE,
                AGGREGATE_TYPE,
                "PAYMENT_CREATED"
        );

        handler.handle(event);

        verifyNoInteractions(simulationPolicy, outboxService);
    }

    @Test
    void shouldRejectEventFromUnsupportedSource() throws Exception {
        var payload = paymentPayload("AUTHORIZATION_PENDING");
        var event = inboxEvent(
                payload,
                "UNKNOWN_SOURCE",
                AGGREGATE_TYPE,
                "PAYMENT_AUTHORIZATION_REQUESTED"
        );

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported inbox event source: UNKNOWN_SOURCE");

        verifyNoInteractions(simulationPolicy, outboxService);
    }

    @Test
    void shouldRejectStatusNotMatchingEventType() throws Exception {
        var payload = paymentPayload("AUTHORIZATION_PENDING");
        var event = inboxEvent(
                payload,
                SOURCE,
                AGGREGATE_TYPE,
                "PAYMENT_CAPTURE_REQUESTED"
        );

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Payment status AUTHORIZATION_PENDING " +
                                "does not match expected status CAPTURE_PENDING"
                );

        verifyNoInteractions(simulationPolicy, outboxService);
    }

    @Test
    void shouldRejectMalformedPayload() {
        var eventId = UUID.randomUUID();

        var event = InboxEvent.pending(
                eventId,
                SOURCE,
                AGGREGATE_TYPE,
                UUID.randomUUID(),
                "PAYMENT_AUTHORIZATION_REQUESTED",
                "{not-valid-json}"
        );

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Cannot deserialize payment command event: " + eventId
                );

        verifyNoInteractions(simulationPolicy, outboxService);
    }

    private PaymentCommandPayload paymentPayload(String status) {
        return new PaymentCommandPayload(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                1_000L,
                "MCR",
                "test-payment",
                status,
                Instant.now()
        );
    }

    private InboxEvent inboxEvent(
            PaymentCommandPayload payload,
            String source,
            String aggregateType,
            String eventType
    ) throws Exception {
        return InboxEvent.pending(
                UUID.randomUUID(),
                source,
                aggregateType,
                payload.paymentId(),
                eventType,
                objectMapper.writeValueAsString(payload)
        );
    }
}