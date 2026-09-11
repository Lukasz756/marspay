package io.github.lukasz756.marspay.earth.inbox;

import io.github.lukasz756.marspay.earth.outbox.OutboxService;
import io.github.lukasz756.marspay.earth.simulation.PaymentSimulationPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class PaymentInboxEventHandler {

    private static final String EXPECTED_SOURCE =
            "MARS_PAYMENT_NODE";

    private static final String PAYMENT_AGGREGATE =
            "PAYMENT";

    private final ObjectMapper objectMapper;
    private final PaymentSimulationPolicy simulationPolicy;
    private final OutboxService outboxService;

    PaymentInboxEventHandler(
            ObjectMapper objectMapper,
            PaymentSimulationPolicy simulationPolicy,
            OutboxService outboxService
    ) {
        this.objectMapper = objectMapper;
        this.simulationPolicy = simulationPolicy;
        this.outboxService = outboxService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(InboxEvent event) {
        validateEnvelope(event);

        PaymentCommandPayload payload =
                deserializePayload(event);

        validatePayload(event, payload);

        if ("PAYMENT_CREATED".equals(event.getEventType())) {
            requireStatus(payload, "CREATED");
            return;
        }

        String expectedStatus =
                expectedRequestStatus(event.getEventType());

        requireStatus(payload, expectedStatus);

        PaymentSimulationPolicy.Decision decision =
                simulationPolicy.decide(
                        payload,
                        event.getEventType(),
                        event.getAttemptCount()
                );

        outboxService.recordPaymentResult(
                payload.paymentId(),
                decision.eventType(),
                decision.reason()
        );
    }

    private void validateEnvelope(InboxEvent event) {
        if (!EXPECTED_SOURCE.equals(event.getSource())) {
            throw new IllegalArgumentException(
                    "Unsupported inbox event source: "
                            + event.getSource()
            );
        }

        if (!PAYMENT_AGGREGATE.equals(event.getAggregateType())) {
            throw new IllegalArgumentException(
                    "Unsupported inbox aggregate type: "
                            + event.getAggregateType()
            );
        }
    }

    private PaymentCommandPayload deserializePayload(
            InboxEvent event
    ) {
        try {
            return objectMapper.readValue(
                    event.getPayload(),
                    PaymentCommandPayload.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Cannot deserialize payment command event: "
                            + event.getEventId(),
                    exception
            );
        }
    }

    private void validatePayload(
            InboxEvent event,
            PaymentCommandPayload payload
    ) {
        if (payload.schemaVersion() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported payment schema version: "
                            + payload.schemaVersion()
            );
        }

        if (payload.paymentId() == null) {
            throw new IllegalArgumentException(
                    "Payment id must not be null"
            );
        }

        if (!payload.paymentId().equals(event.getAggregateId())) {
            throw new IllegalArgumentException(
                    "Payment id does not match aggregate id"
            );
        }

        if (payload.sourceBalanceAccountId() == null
                || payload.targetBalanceAccountId() == null) {
            throw new IllegalArgumentException(
                    "Payment account ids must not be null"
            );
        }

        if (payload.sourceBalanceAccountId()
                .equals(payload.targetBalanceAccountId())) {
            throw new IllegalArgumentException(
                    "Payment account ids must be different"
            );
        }

        if (payload.amountMinor() <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be greater than zero"
            );
        }

        if (!"MCR".equals(payload.currency())) {
            throw new IllegalArgumentException(
                    "Unsupported payment currency: "
                            + payload.currency()
            );
        }

        if (payload.reference() == null
                || payload.reference().isBlank()) {
            throw new IllegalArgumentException(
                    "Payment reference must not be blank"
            );
        }

        if (payload.status() == null || payload.status().isBlank()) {
            throw new IllegalArgumentException(
                    "Payment status must not be blank"
            );
        }

        if (payload.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "Payment occurredAt must not be null"
            );
        }
    }

    private String expectedRequestStatus(String eventType) {
        return switch (eventType) {
            case "PAYMENT_AUTHORIZATION_REQUESTED" ->
                    "AUTHORIZATION_PENDING";

            case "PAYMENT_CAPTURE_REQUESTED" ->
                    "CAPTURE_PENDING";

            case "PAYMENT_CANCEL_REQUESTED" ->
                    "CANCEL_PENDING";

            case "PAYMENT_REFUND_REQUESTED" ->
                    "REFUND_PENDING";

            default -> throw new IllegalArgumentException(
                    "Unsupported payment command event: "
                            + eventType
            );
        };
    }

    private void requireStatus(
            PaymentCommandPayload payload,
            String expectedStatus
    ) {
        if (!expectedStatus.equals(payload.status())) {
            throw new IllegalArgumentException(
                    "Payment status "
                            + payload.status()
                            + " does not match expected status "
                            + expectedStatus
            );
        }
    }
}