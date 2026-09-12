package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.inbox.InboxService;
import io.github.lukasz756.marspay.paymentnode.inbox.IncomingEventRequest;
import io.github.lukasz756.marspay.paymentnode.inbox.PaymentResultPayload;
import io.github.lukasz756.marspay.paymentnode.testsupport.AbstractPaymentIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentInboxIT extends AbstractPaymentIT {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private PaymentOperationRepository paymentOperationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void storesAndProcessesDuplicatePaymentEventOnlyOnce()
            throws Exception {

        PaymentFixture fixture = createPaymentFixture();

        UUID sourceAccountId =
                fixture.sourceAccount().getId();

        UUID targetAccountId =
                fixture.targetAccount().getId();

        accountService.creditBalanceAccount(
                sourceAccountId,
                1_000,
                "inbox-funding-" + UUID.randomUUID()
        );

        Payment payment = paymentService.createPayment(
                sourceAccountId,
                targetAccountId,
                300,
                "inbox-payment-" + UUID.randomUUID()
        );

        mockMvc.perform(
                        post(
                                "/api/payments/{paymentId}/authorize",
                                payment.getId()
                        )
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        jsonPath("$.status")
                                .value("AUTHORIZATION_PENDING")
                );

        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        PaymentResultPayload payload =
                new PaymentResultPayload(
                        1,
                        payment.getId(),
                        "AUTHORIZED",
                        null,
                        occurredAt
                );

        IncomingEventRequest request =
                new IncomingEventRequest(
                        eventId,
                        "EARTH_PAYMENT_SERVICE",
                        "PAYMENT",
                        payment.getId(),
                        "PAYMENT_AUTHORIZED",
                        objectMapper.valueToTree(payload)
                );

        byte[] requestBody =
                objectMapper.writeValueAsBytes(request);

        mockMvc.perform(
                        post("/internal/relay/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        header().string(
                                "Inbox-Duplicate",
                                "false"
                        )
                );

        mockMvc.perform(
                        post("/internal/relay/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        header().string(
                                "Inbox-Duplicate",
                                "true"
                        )
                );

        Long storedEvents = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM inbox_event
                        WHERE event_id = ?
                        """,
                Long.class,
                eventId
        );

        assertThat(storedEvents).isEqualTo(1L);

        inboxService.processEvent(
                eventId,
                Instant.now()
        );

        inboxService.processEvent(
                eventId,
                Instant.now()
        );

        mockMvc.perform(
                        get(
                                "/api/payments/{paymentId}",
                                payment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status")
                                .value("AUTHORIZED")
                );

        assertThat(
                paymentOperationRepository
                        .findAllByPaymentIdOrderByCreatedAtAsc(
                                payment.getId()
                        )
        )
                .extracting(PaymentOperation::getType)
                .containsExactly(
                        PaymentOperationType.CREATE,
                        PaymentOperationType.AUTHORIZATION_REQUESTED,
                        PaymentOperationType.AUTHORIZATION_CONFIRMED
                );

        String inboxStatus = jdbcTemplate.queryForObject(
                """
                        SELECT status
                        FROM inbox_event
                        WHERE event_id = ?
                        """,
                String.class,
                eventId
        );

        assertThat(inboxStatus).isEqualTo("PROCESSED");

        BalanceAccount sourceAccount =
                accountService.getBalanceAccount(sourceAccountId);

        assertThat(
                sourceAccount.getAvailableBalanceMinor()
        ).isEqualTo(700);

        assertThat(
                sourceAccount.getReservedBalanceMinor()
        ).isEqualTo(300);
    }
}