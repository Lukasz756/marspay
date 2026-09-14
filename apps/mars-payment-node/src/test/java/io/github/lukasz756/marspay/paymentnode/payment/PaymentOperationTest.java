package io.github.lukasz756.marspay.paymentnode.payment;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentOperationTest {

    @Test
    void recordsNormalizedProcessingReasonForFailedOperation() {
        PaymentOperation operation = PaymentOperation.record(UUID.randomUUID(), PaymentOperationType.CAPTURE_FAILED,
                                                             300, "  ACCOUNT_SUSPENDED  ");

        assertThat(operation.getProcessingReason()).isEqualTo("ACCOUNT_SUSPENDED");
    }

    @Test
    void rejectsBlankProcessingReason() {
        assertThatThrownBy(() -> PaymentOperation.record(UUID.randomUUID(), PaymentOperationType.CAPTURE_FAILED, 300,
                                                         "   ")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment operation processing reason must not be blank");
    }
}
