package io.github.lukasz756.marspay.earth.simulation;

import io.github.lukasz756.marspay.earth.inbox.PaymentCommandPayload;
import io.github.lukasz756.marspay.earth.outbox.OutboxEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSimulationPolicyTest {

    private final PaymentSimulationPolicy policy = new PaymentSimulationPolicy();

    @Test
    void declinesRiskyAuthorization() {
        PaymentCommandPayload payload = paymentPayload(
                "risk-declined-order",
                "AUTHORIZATION_PENDING"
        );

        PaymentSimulationPolicy.Decision decision = policy.decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                0
        );

        assertThat(decision.eventType())
                .isEqualTo(OutboxEventType.PAYMENT_DECLINED);

        assertThat(decision.reason())
                .isEqualTo("RISK_DECLINED");
    }

    @Test
    void failsCaptureForSuspendedAccount() {
        PaymentCommandPayload payload = paymentPayload(
                "account-suspended-order",
                "CAPTURE_PENDING"
        );

        PaymentSimulationPolicy.Decision decision = policy.decide(
                payload,
                "PAYMENT_CAPTURE_REQUESTED",
                0
        );

        assertThat(decision.eventType())
                .isEqualTo(OutboxEventType.PAYMENT_CAPTURE_FAILED);

        assertThat(decision.reason())
                .isEqualTo("ACCOUNT_SUSPENDED");
    }

    @Test
    void allowsAuthorizationForSuspendedAccount() {
        PaymentCommandPayload payload = paymentPayload(
                "account-suspended-order",
                "AUTHORIZATION_PENDING"
        );

        PaymentSimulationPolicy.Decision decision = policy.decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                0
        );

        assertThat(decision.eventType())
                .isEqualTo(OutboxEventType.PAYMENT_AUTHORIZED);

        assertThat(decision.reason()).isNull();
    }

    @Test
    void failsFirstAttemptWithTemporaryProcessorTimeout() {
        PaymentCommandPayload payload = paymentPayload(
                "order-timeout-once",
                "AUTHORIZATION_PENDING"
        );

        assertThatThrownBy(() -> policy.decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                0
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated one-time processor timeout");
    }

    @Test
    void succeedsWhenTemporaryProcessorTimeoutIsRetried() {
        PaymentCommandPayload payload = paymentPayload(
                "order-timeout-once",
                "AUTHORIZATION_PENDING"
        );

        PaymentSimulationPolicy.Decision decision = policy.decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                1
        );

        assertThat(decision.eventType())
                .isEqualTo(OutboxEventType.PAYMENT_AUTHORIZED);

        assertThat(decision.reason()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 4})
    void failsEveryAttemptWhenProcessorIsDown(int attemptCount) {
        PaymentCommandPayload payload = paymentPayload(
                "order-processor-down",
                "AUTHORIZATION_PENDING"
        );

        assertThatThrownBy(() -> policy.decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                attemptCount
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Simulated unavailable processor");
    }

    @ParameterizedTest
    @MethodSource("successfulRequests")
    void returnsExpectedResultForForcedSuccessfulPayment(
            String requestEventType,
            String paymentStatus,
            OutboxEventType expectedResult
    ) {
        PaymentCommandPayload payload = paymentPayload(
                "payment-ok",
                paymentStatus
        );

        PaymentSimulationPolicy.Decision decision = policy.decide(
                payload,
                requestEventType,
                0
        );

        assertThat(decision.eventType()).isEqualTo(expectedResult);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void rejectsNullPaymentPayload() {
        assertThatThrownBy(() -> policy.decide(
                null,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment payload must not be null");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankRequestEventType(String requestEventType) {
        PaymentCommandPayload payload = paymentPayload(
                "payment-ok",
                "AUTHORIZATION_PENDING"
        );

        assertThatThrownBy(() -> policy.decide(
                payload,
                requestEventType,
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Request event type must not be blank");
    }

    @Test
    void rejectsNegativeAttemptCount() {
        PaymentCommandPayload payload = paymentPayload(
                "payment-ok",
                "AUTHORIZATION_PENDING"
        );

        assertThatThrownBy(() -> policy.decide(
                payload,
                "PAYMENT_AUTHORIZATION_REQUESTED",
                -1
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attempt count must not be negative");
    }

    @Test
    void rejectsUnsupportedRequestEventType() {
        PaymentCommandPayload payload = paymentPayload(
                "payment-ok",
                "UNKNOWN"
        );

        assertThatThrownBy(() -> policy.decide(
                payload,
                "PAYMENT_UNKNOWN_REQUESTED",
                0
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Unsupported payment request event: PAYMENT_UNKNOWN_REQUESTED"
                );
    }

    private static Stream<Arguments> successfulRequests() {
        return Stream.of(
                Arguments.of(
                        "PAYMENT_AUTHORIZATION_REQUESTED",
                        "AUTHORIZATION_PENDING",
                        OutboxEventType.PAYMENT_AUTHORIZED
                ),
                Arguments.of(
                        "PAYMENT_CAPTURE_REQUESTED",
                        "CAPTURE_PENDING",
                        OutboxEventType.PAYMENT_CAPTURED
                ),
                Arguments.of(
                        "PAYMENT_CANCEL_REQUESTED",
                        "CANCEL_PENDING",
                        OutboxEventType.PAYMENT_CANCELLED
                ),
                Arguments.of(
                        "PAYMENT_REFUND_REQUESTED",
                        "REFUND_PENDING",
                        OutboxEventType.PAYMENT_REFUNDED
                )
        );
    }

    private PaymentCommandPayload paymentPayload(
            String reference,
            String status
    ) {
        return new PaymentCommandPayload(
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                300L,
                "MCR",
                reference,
                status,
                Instant.now()
        );
    }
}