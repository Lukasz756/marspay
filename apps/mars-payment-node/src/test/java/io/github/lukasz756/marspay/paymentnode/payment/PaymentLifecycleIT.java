package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.inbox.InboxService;
import io.github.lukasz756.marspay.paymentnode.inbox.IncomingEventRequest;
import io.github.lukasz756.marspay.paymentnode.inbox.PaymentResultPayload;
import io.github.lukasz756.marspay.paymentnode.ledger.LedgerService;
import io.github.lukasz756.marspay.paymentnode.ledger.LedgerTransactionResponse;
import io.github.lukasz756.marspay.paymentnode.ledger.LedgerTransactionType;
import io.github.lukasz756.marspay.paymentnode.testsupport.AbstractPaymentIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentLifecycleIT extends AbstractPaymentIT {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private PaymentOperationRepository paymentOperationRepository;

    @Autowired
    private LedgerService ledgerService;

    @Test
    void authorizesAndCapturesPaymentThroughHttpAndInbox() throws Exception {

        PaymentFixture fixture = createPaymentFixture();

        UUID sourceAccountId = fixture.sourceAccount()
                .getId();

        UUID targetAccountId = fixture.targetAccount()
                .getId();

        accountService.creditBalanceAccount(sourceAccountId, 1_000, "lifecycle-funding-" + UUID.randomUUID());

        Payment payment = paymentService.createPayment(sourceAccountId, targetAccountId, 300,
                                                       "lifecycle-" + UUID.randomUUID());

        mockMvc.perform(post("/api/payments/{paymentId}/authorize", payment.getId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(payment.getId()
                                                          .toString()))
                .andExpect(jsonPath("$.status").value("AUTHORIZATION_PENDING"));

        BalanceAccount sourceAfterAuthorizationRequest = accountService.getBalanceAccount(sourceAccountId);

        assertThat(sourceAfterAuthorizationRequest.getAvailableBalanceMinor()).isEqualTo(700);

        assertThat(sourceAfterAuthorizationRequest.getReservedBalanceMinor()).isEqualTo(300);

        receiveAndProcessPaymentResult(payment.getId(), "PAYMENT_AUTHORIZED", "AUTHORIZED");

        mockMvc.perform(get("/api/payments/{paymentId}", payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        mockMvc.perform(post("/api/payments/{paymentId}/capture", payment.getId()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("CAPTURE_PENDING"));

        receiveAndProcessPaymentResult(payment.getId(), "PAYMENT_CAPTURED", "CAPTURED");

        mockMvc.perform(get("/api/payments/{paymentId}", payment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"))
                .andExpect(jsonPath("$.amountMinor").value(300));

        BalanceAccount sourceAfterCapture = accountService.getBalanceAccount(sourceAccountId);

        BalanceAccount targetAfterCapture = accountService.getBalanceAccount(targetAccountId);

        assertThat(sourceAfterCapture.getAvailableBalanceMinor()).isEqualTo(700);

        assertThat(sourceAfterCapture.getReservedBalanceMinor()).isZero();

        assertThat(targetAfterCapture.getAvailableBalanceMinor()).isEqualTo(300);

        assertThat(targetAfterCapture.getReservedBalanceMinor()).isZero();

        assertThat(paymentOperationRepository.findAllByPaymentIdOrderByCreatedAtAsc(payment.getId())).extracting(
                        PaymentOperation::getType)
                .containsExactly(PaymentOperationType.CREATE, PaymentOperationType.AUTHORIZATION_REQUESTED,
                                 PaymentOperationType.AUTHORIZATION_CONFIRMED, PaymentOperationType.CAPTURE_REQUESTED
                        , PaymentOperationType.CAPTURE_CONFIRMED);

        List<LedgerTransactionResponse> ledger = ledgerService.getPaymentLedger(payment.getId());

        assertThat(ledger).extracting(LedgerTransactionResponse::type)
                .containsExactly(LedgerTransactionType.PAYMENT_AUTHORIZE, LedgerTransactionType.PAYMENT_CAPTURE);

        assertThat(ledger).allSatisfy(transaction -> {
            assertThat(transaction.entries()).hasSize(2);

            long total = transaction.entries()
                    .stream()
                    .mapToLong(entry -> entry.amountMinor())
                    .sum();

            assertThat(total).isZero();
        });
    }

    private void receiveAndProcessPaymentResult(UUID paymentId, String eventType, String paymentStatus) throws Exception {

        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        PaymentResultPayload payload = new PaymentResultPayload(1, paymentId, paymentStatus, null, occurredAt);

        IncomingEventRequest request = new IncomingEventRequest(eventId, "EARTH_PAYMENT_SERVICE", "PAYMENT",
                                                                paymentId, eventType,
                                                                objectMapper.valueToTree(payload));

        mockMvc.perform(post("/internal/relay/events").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Inbox-Duplicate", "false"));

        inboxService.processEvent(eventId, Instant.now());
    }
}