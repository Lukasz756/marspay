package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.ledger.LedgerService;
import io.github.lukasz756.marspay.paymentnode.testsupport.AbstractPaymentIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentAuthorizationIT extends AbstractPaymentIT {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentOperationRepository paymentOperationRepository;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsAuthorizationWhenBalanceIsInsufficientAndRollsBackAllChanges()
            throws Exception {

        PaymentFixture fixture = createPaymentFixture();

        UUID sourceAccountId =
                fixture.sourceAccount().getId();

        UUID targetAccountId =
                fixture.targetAccount().getId();

        accountService.creditBalanceAccount(
                sourceAccountId,
                100,
                "authorization-funding-" + UUID.randomUUID()
        );

        Payment payment = paymentService.createPayment(
                sourceAccountId,
                targetAccountId,
                300,
                "insufficient-balance-" + UUID.randomUUID()
        );

        long outboxCountBefore =
                countOutboxEvents(payment.getId());

        assertThat(
                ledgerService.getPaymentLedger(payment.getId())
        ).isEmpty();

        mockMvc.perform(
                        post(
                                "/api/payments/{paymentId}/authorize",
                                payment.getId()
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.title")
                                .value("Insufficient balance")
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Insufficient balance on account %s: "
                                        .formatted(sourceAccountId)
                                        + "available=100, requested=300"
                        )
                );

        mockMvc.perform(
                        get(
                                "/api/payments/{paymentId}",
                                payment.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.status").value("CREATED")
                );

        BalanceAccount sourceAccountAfterFailure =
                accountService.getBalanceAccount(sourceAccountId);

        BalanceAccount targetAccountAfterFailure =
                accountService.getBalanceAccount(targetAccountId);

        assertThat(
                sourceAccountAfterFailure.getAvailableBalanceMinor()
        ).isEqualTo(100);

        assertThat(
                sourceAccountAfterFailure.getReservedBalanceMinor()
        ).isZero();

        assertThat(
                targetAccountAfterFailure.getAvailableBalanceMinor()
        ).isZero();

        assertThat(
                targetAccountAfterFailure.getReservedBalanceMinor()
        ).isZero();

        assertThat(
                paymentOperationRepository
                        .findAllByPaymentIdOrderByCreatedAtAsc(
                                payment.getId()
                        )
        )
                .extracting(PaymentOperation::getType)
                .containsExactly(PaymentOperationType.CREATE);

        assertThat(
                ledgerService.getPaymentLedger(payment.getId())
        ).isEmpty();

        assertThat(countOutboxEvents(payment.getId()))
                .isEqualTo(outboxCountBefore);
    }

    private long countOutboxEvents(UUID paymentId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM outbox_event
                        WHERE aggregate_id = ?
                        """,
                Long.class,
                paymentId
        );

        return count == null ? 0 : count;
    }
}