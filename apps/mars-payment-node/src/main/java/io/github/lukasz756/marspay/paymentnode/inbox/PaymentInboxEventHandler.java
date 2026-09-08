package io.github.lukasz756.marspay.paymentnode.inbox;

import io.github.lukasz756.marspay.paymentnode.payment.PaymentService;
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

        PaymentResultPayload payload =
                deserializePayload(event);

        validatePayload(event, payload);

        switch (event.getEventType()) {
            case "PAYMENT_AUTHORIZED" -> {
                requireStatus(payload, "AUTHORIZED");
                paymentService.confirmAuthorization(payload.paymentId());
            }

            case "PAYMENT_DECLINED" -> {
                requireStatus(payload, "DECLINED");
                paymentService.declineAuthorization(payload.paymentId());
            }

            case "PAYMENT_CAPTURED" -> {
                requireStatus(payload, "CAPTURED");
                paymentService.confirmCapture(payload.paymentId());
            }

            case "PAYMENT_CAPTURE_FAILED" -> {
                requireStatus(payload, "CAPTURE_FAILED");
                paymentService.failCapture(payload.paymentId());
            }

            case "PAYMENT_CANCELLED" -> {
                requireStatus(payload, "CANCELLED");
                paymentService.confirmCancel(payload.paymentId());
            }

            case "PAYMENT_CANCEL_FAILED" -> {
                requireStatus(payload, "CANCEL_FAILED");
                paymentService.failCancel(payload.paymentId());
            }

            case "PAYMENT_REFUNDED" -> {
                requireStatus(payload, "REFUNDED");
                paymentService.confirmRefund(payload.paymentId());
            }

            case "PAYMENT_REFUND_FAILED" -> {
                requireStatus(payload, "REFUND_FAILED");
                paymentService.failRefund(payload.paymentId());
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

    private PaymentResultPayload deserializePayload(
            InboxEvent event
    ) {
        try {
            return objectMapper.readValue(
                    event.getPayload(),
                    PaymentResultPayload.class
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
            PaymentResultPayload payload
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

        if (payload.status() == null || payload.status().isBlank()) {
            throw new IllegalArgumentException(
                    "Payment event status must not be blank"
            );
        }

        if (payload.occurredAt() == null) {
            throw new IllegalArgumentException(
                    "Payment event occurredAt must not be null"
            );
        }
    }

    private void requireStatus(
            PaymentResultPayload payload,
            String expectedStatus
    ) {
        if (!expectedStatus.equals(payload.status())) {
            throw new IllegalArgumentException(
                    "Payment event status "
                            + payload.status()
                            + " does not match expected status "
                            + expectedStatus
            );
        }
    }
}