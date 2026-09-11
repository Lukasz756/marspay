package io.github.lukasz756.marspay.earth.simulation;

import io.github.lukasz756.marspay.earth.inbox.PaymentCommandPayload;
import io.github.lukasz756.marspay.earth.outbox.OutboxEventType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

@Component
public class PaymentSimulationPolicy {

    public Decision decide(
            PaymentCommandPayload payload,
            String requestEventType,
            int attemptCount
    ) {
        if (payload == null) {
            throw new IllegalArgumentException(
                    "Payment payload must not be null"
            );
        }

        if (requestEventType == null || requestEventType.isBlank()) {
            throw new IllegalArgumentException(
                    "Request event type must not be blank"
            );
        }

        if (attemptCount < 0) {
            throw new IllegalArgumentException(
                    "Attempt count must not be negative"
            );
        }

        String reference = payload.reference()
                .trim()
                .toLowerCase(Locale.ROOT);

        Decision forcedDecision = forcedDecision(
                reference,
                requestEventType,
                attemptCount
        );

        if (forcedDecision != null) {
            return forcedDecision;
        }

        int bucket = Math.floorMod(
                Objects.hash(
                        payload.paymentId(),
                        requestEventType
                ),
                100
        );

        if (bucket < 80) {
            return success(requestEventType);
        }

        if (bucket < 90) {
            return businessFailure(requestEventType);
        }

        if (bucket < 95) {
            if (attemptCount < 2) {
                throw new IllegalStateException(
                        "Simulated temporary processor failure"
                );
            }

            return success(requestEventType);
        }

        throw new IllegalStateException(
                "Simulated permanent processor failure"
        );
    }

    private Decision forcedDecision(
            String reference,
            String requestEventType,
            int attemptCount
    ) {
        if (reference.contains("risk-declined")) {
            if ("PAYMENT_AUTHORIZATION_REQUESTED"
                    .equals(requestEventType)) {
                return new Decision(
                        OutboxEventType.PAYMENT_DECLINED,
                        "RISK_DECLINED"
                );
            }
        }

        if (reference.contains("account-suspended")) {
            if ("PAYMENT_CAPTURE_REQUESTED"
                    .equals(requestEventType)) {
                return new Decision(
                        OutboxEventType.PAYMENT_CAPTURE_FAILED,
                        "ACCOUNT_SUSPENDED"
                );
            }

            return success(requestEventType);
        }

        if (reference.contains("timeout-once")) {
            if (attemptCount == 0) {
                throw new IllegalStateException(
                        "Simulated one-time processor timeout"
                );
            }

            return success(requestEventType);
        }

        if (reference.contains("processor-down")) {
            throw new IllegalStateException(
                    "Simulated unavailable processor"
            );
        }

        if (reference.endsWith("-ok")) {
            return success(requestEventType);
        }

        return null;
    }

    private Decision success(String requestEventType) {
        return switch (requestEventType) {
            case "PAYMENT_AUTHORIZATION_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_AUTHORIZED,
                            null
                    );

            case "PAYMENT_CAPTURE_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_CAPTURED,
                            null
                    );

            case "PAYMENT_CANCEL_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_CANCELLED,
                            null
                    );

            case "PAYMENT_REFUND_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_REFUNDED,
                            null
                    );

            default -> throw new IllegalArgumentException(
                    "Unsupported payment request event: "
                            + requestEventType
            );
        };
    }

    private Decision businessFailure(String requestEventType) {
        return switch (requestEventType) {
            case "PAYMENT_AUTHORIZATION_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_DECLINED,
                            "RISK_DECLINED"
                    );

            case "PAYMENT_CAPTURE_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_CAPTURE_FAILED,
                            "ACCOUNT_SUSPENDED"
                    );

            case "PAYMENT_CANCEL_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_CANCEL_FAILED,
                            "CANCEL_TOO_LATE"
                    );

            case "PAYMENT_REFUND_REQUESTED" ->
                    new Decision(
                            OutboxEventType.PAYMENT_REFUND_FAILED,
                            "REFUND_WINDOW_EXPIRED"
                    );

            default -> throw new IllegalArgumentException(
                    "Unsupported payment request event: "
                            + requestEventType
            );
        };
    }

    public record Decision(
            OutboxEventType eventType,
            String reason
    ) {
    }
}