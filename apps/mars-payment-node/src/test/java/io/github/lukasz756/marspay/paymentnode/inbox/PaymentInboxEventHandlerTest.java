package io.github.lukasz756.marspay.paymentnode.inbox;

import io.github.lukasz756.marspay.paymentnode.payment.PaymentService;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentInboxEventHandlerTest {

    @Mock
    private PaymentService paymentService;

    private ObjectMapper objectMapper;

    private PaymentInboxEventHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new PaymentInboxEventHandler(paymentService, objectMapper);
    }

    @Test
    void forwardsEarthProcessingReasonForFailedPaymentResult() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResultPayload payload = new PaymentResultPayload(1, paymentId, "CAPTURE_FAILED",
                                                                "ACCOUNT_SUSPENDED", Instant.now());

        InboxEvent event = paymentResultEvent(paymentId, "PAYMENT_CAPTURE_FAILED", payload);

        handler.handle(event);

        verify(paymentService).failCapture(paymentId, "ACCOUNT_SUSPENDED");
        verifyNoMoreInteractions(paymentService);
    }

    @Test
    void rejectsFailedPaymentResultWithoutProcessingReason() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentResultPayload payload = new PaymentResultPayload(1, paymentId, "DECLINED", null, Instant.now());

        InboxEvent event = paymentResultEvent(paymentId, "PAYMENT_DECLINED", payload);

        assertThatThrownBy(() -> handler.handle(event)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed payment event processing reason must not be blank");

        verifyNoInteractions(paymentService);
    }

    private InboxEvent paymentResultEvent(UUID paymentId, String eventType, PaymentResultPayload payload)
            throws Exception {
        return InboxEvent.pending(UUID.randomUUID(), "EARTH_PAYMENT_SERVICE", "PAYMENT", paymentId, eventType,
                                  objectMapper.writeValueAsString(payload));
    }
}
