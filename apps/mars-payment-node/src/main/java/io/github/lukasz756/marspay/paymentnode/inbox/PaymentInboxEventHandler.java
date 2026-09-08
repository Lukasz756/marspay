package io.github.lukasz756.marspay.paymentnode.inbox;

import io.github.lukasz756.marspay.paymentnode.payment.PaymentService;
import io.github.lukasz756.marspay.paymentnode.payment.PaymentStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
class PaymentInboxEventHandler {

    private static final String EXPECTED_SOURCE =
            "EARTH_PAYMENT_SERVICE";

    private static final String PAYMENT_AGGREGATE =
            "PAYMENT";

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    PaymentInboxEventHandler(
            PaymentService paymentService,
            ObjectMapper objectMapper
    ) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(InboxEvent event) {
        validateEnvelope(event);

        PaymentAuthorizationResultPayload payload =
                deserializePayload(event);

        validatePayload(event, payload);

        switch (event.getEventType()) {
            case "PAYMENT_AUTHORIZED" -> {
                requireStatus(payload, PaymentStatus.AUTHORIZED);
                paymentService.confirmAuthorization(payload.paymentId());
            }

            case "PAYMENT_DECLINED" -> {
                requireStatus(payload, PaymentStatus.DECLINED);
                paymentService.declineAuthorization(payload.paymentId());
            }

            default -> throw new IllegalArgumentException(
                    "Unsupported payment inbox event type: "
                            + event.getEventType()
            );
        }
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

    private PaymentAuthorizationResultPayload deserializePayload(
            InboxEvent event
    ) {
        try {
            return objectMapper.readValue(
                    event.getPayload(),
                    PaymentAuthorizationResultPayload.class
            );
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Cannot deserialize payment inbox event: "
                            + event.getEventId(),
                    exception
            );
        }
    }

    private void validatePayload(
            InboxEvent event,
            PaymentAuthorizationResultPayload payload
    ) {
        if (payload.schemaVersion() != 1) {
            throw new IllegalArgumentException(
                    "Unsupported payment event schema version: "
                            + payload.schemaVersion()
            );
        }

        if (payload.paymentId() == null) {
            throw new IllegalArgumentException(
                    "Payment event payment id must not be null"
            );
        }

        if (!payload.paymentId().equals(event.getAggregateId())) {
            throw new IllegalArgumentException(
                    "Payment event aggregate id does not match payload"
            );
        }

        if (payload.status() == null) {
            throw new IllegalArgumentException(
                    "Payment event status must not be null"
            );
        }

        if (payload.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "Payment event occurredAt must not be null"
            );
        }
    }

    private void requireStatus(
            PaymentAuthorizationResultPayload payload,
            PaymentStatus expectedStatus
    ) {
        if (payload.status() != expectedStatus) {
            throw new IllegalArgumentException(
                    "Payment event status "
                            + payload.status()
                            + " does not match expected status "
                            + expectedStatus
            );
        }
    }
}