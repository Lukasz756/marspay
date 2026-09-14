package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.payment.exceptions.PaymentInvalidStatusException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void movesCapturedPaymentToRefundPending() {
        Payment capturedPayment = createCapturedPayment();

        capturedPayment.requestRefund();

        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }

    @Test
    void movesPendingRefundToRefundedWhenConfirmed() {
        Payment capturedPayment = createCapturedPayment();

        capturedPayment.requestRefund();
        capturedPayment.confirmRefund();

        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void movesPendingRefundBackToCapturedWhenFailed() {
        Payment capturedPayment = createCapturedPayment();

        capturedPayment.requestRefund();
        capturedPayment.failRefund("REFUND_WINDOW_EXPIRED");

        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(capturedPayment.getProcessingReason()).isEqualTo("REFUND_WINDOW_EXPIRED");
    }

    @Test
    void rejectsRefundRequestWhenPaymentIsNotCaptured() {
        Payment createdPayment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), 300, "MCR", "unit-test-payment");

        assertThatThrownBy(() -> createdPayment.requestRefund()).isInstanceOf(PaymentInvalidStatusException.class);

        assertThat(createdPayment.getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void rejectsRefundConfirmationWhenRefundIsNotPending() {
        Payment capturedPayment = createCapturedPayment();

        assertThatThrownBy(() -> capturedPayment.confirmRefund()).isInstanceOf(PaymentInvalidStatusException.class);

        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void rejectsRefundFailureWhenRefundIsNotPending() {
        Payment capturedPayment = createCapturedPayment();

        assertThatThrownBy(() -> capturedPayment.failRefund("REFUND_WINDOW_EXPIRED")).isInstanceOf(
                PaymentInvalidStatusException.class);

        assertThat(capturedPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    @Test
    void rejectsSecondRefundRequestWhileRefundIsPending() {
        Payment payment = createCapturedPayment();

        payment.requestRefund();

        assertThatThrownBy(payment::requestRefund).isInstanceOf(PaymentInvalidStatusException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }

    @Test
    void clearsPreviousProcessingReasonWhenFailedOperationIsRetried() {
        Payment payment = createCapturedPayment();

        payment.requestRefund();
        payment.failRefund("  REFUND_WINDOW_EXPIRED  ");

        assertThat(payment.getProcessingReason()).isEqualTo("REFUND_WINDOW_EXPIRED");

        payment.requestRefund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        assertThat(payment.getProcessingReason()).isNull();
    }

    @Test
    void rejectsBlankProcessingReasonWithoutChangingPendingPayment() {
        Payment payment = createCapturedPayment();

        payment.requestRefund();

        assertThatThrownBy(() -> payment.failRefund("   ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment processing failure reason must not be blank");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }

    private Payment createCapturedPayment() {
        Payment payment = Payment.create(UUID.randomUUID(), UUID.randomUUID(), 300, "MCR", "unit-test-payment");

        payment.requestAuthorization();
        payment.confirmAuthorization();
        payment.requestCapture();
        payment.confirmCapture();

        return payment;
    }
}
