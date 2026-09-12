package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.AccountHolder;
import io.github.lukasz756.marspay.paymentnode.account.AccountHolderType;
import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.testsupport.AbstractPaymentIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


class PaymentControllerIT extends AbstractPaymentIT {


    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentOperationRepository paymentOperationRepository;

    @Test
    void createsPaymentOnlyOnceWhenRequestIsReplayedWithSameIdempotencyKey()
            throws Exception {

        PaymentFixture fixture = createPaymentFixture();

        String idempotencyKey =
                "payment-controller-" + UUID.randomUUID();

        String paymentReference =
                "payment-" + UUID.randomUUID();

        CreatePaymentRequest request = new CreatePaymentRequest(
                fixture.sourceAccount().getId(),
                fixture.targetAccount().getId(),
                300,
                paymentReference
        );

        byte[] requestBody =
                objectMapper.writeValueAsBytes(request);

        long paymentCountBefore =
                paymentRepository.count();

        MvcResult firstResult = mockMvc.perform(
                        post("/api/payments")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(
                        header().doesNotExist("Idempotent-Replayed")
                )
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(
                        jsonPath("$.sourceBalanceAccountId")
                                .value(
                                        fixture.sourceAccount()
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$.targetBalanceAccountId")
                                .value(
                                        fixture.targetAccount()
                                                .getId()
                                                .toString()
                                )
                )
                .andExpect(jsonPath("$.amountMinor").value(300))
                .andExpect(jsonPath("$.currency").value("MCR"))
                .andExpect(
                        jsonPath("$.reference")
                                .value(paymentReference)
                )
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn();

        String firstResponseBody =
                firstResult.getResponse().getContentAsString();

        PaymentResponse createdPayment =
                objectMapper.readValue(
                        firstResponseBody,
                        PaymentResponse.class
                );

        String expectedLocation =
                "/api/payments/" + createdPayment.id();

        assertThat(
                firstResult.getResponse()
                        .getHeader(HttpHeaders.LOCATION)
        ).isEqualTo(expectedLocation);

        assertThat(paymentRepository.count())
                .isEqualTo(paymentCountBefore + 1);

        MvcResult replayResult = mockMvc.perform(
                        post("/api/payments")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(
                        header().string(
                                HttpHeaders.LOCATION,
                                expectedLocation
                        )
                )
                .andExpect(
                        header().string(
                                "Idempotent-Replayed",
                                "true"
                        )
                )
                .andReturn();

        assertThat(
                replayResult.getResponse().getContentAsString()
        ).isEqualTo(firstResponseBody);

        assertThat(paymentRepository.count())
                .isEqualTo(paymentCountBefore + 1);

        assertThat(
                paymentOperationRepository
                        .findAllByPaymentIdOrderByCreatedAtAsc(
                                createdPayment.id()
                        )
        )
                .extracting(PaymentOperation::getType)
                .containsExactly(PaymentOperationType.CREATE);

        mockMvc.perform(
                        get("/api/payments/{paymentId}",
                                createdPayment.id())
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(createdPayment.id().toString())
                )
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(
                        jsonPath("$.reference")
                                .value(paymentReference)
                );
    }

    @Test
    void rejectsReusingIdempotencyKeyWithDifferentRequestData()
            throws Exception {

        PaymentFixture fixture = createPaymentFixture();

        String idempotencyKey =
                "payment-controller-conflict-" + UUID.randomUUID();

        String paymentReference =
                "payment-conflict-" + UUID.randomUUID();

        CreatePaymentRequest originalRequest =
                new CreatePaymentRequest(
                        fixture.sourceAccount().getId(),
                        fixture.targetAccount().getId(),
                        300,
                        paymentReference
                );

        CreatePaymentRequest changedRequest =
                new CreatePaymentRequest(
                        fixture.sourceAccount().getId(),
                        fixture.targetAccount().getId(),
                        400,
                        paymentReference
                );

        long paymentCountBefore =
                paymentRepository.count();

        MvcResult creationResult = mockMvc.perform(
                        post("/api/payments")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsBytes(
                                                originalRequest
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andReturn();

        PaymentResponse createdPayment =
                objectMapper.readValue(
                        creationResult
                                .getResponse()
                                .getContentAsString(),
                        PaymentResponse.class
                );

        mockMvc.perform(
                        post("/api/payments")
                                .header(
                                        "Idempotency-Key",
                                        idempotencyKey
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsBytes(
                                                changedRequest
                                        )
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
                                .value("Idempotency key conflict")
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Idempotency key '"
                                        + idempotencyKey
                                        + "' was already used with "
                                        + "different request data"
                        )
                );

        assertThat(paymentRepository.count())
                .isEqualTo(paymentCountBefore + 1);

        assertThat(
                paymentOperationRepository
                        .findAllByPaymentIdOrderByCreatedAtAsc(
                                createdPayment.id()
                        )
        )
                .extracting(PaymentOperation::getType)
                .containsExactly(PaymentOperationType.CREATE);
    }

    @Test
    void rejectsMissingOrBlankIdempotencyKeyWithoutPersistingPayment()
            throws Exception {

        PaymentFixture fixture = createPaymentFixture();

        CreatePaymentRequest request =
                new CreatePaymentRequest(
                        fixture.sourceAccount().getId(),
                        fixture.targetAccount().getId(),
                        300,
                        "payment-validation-" + UUID.randomUUID()
                );

        byte[] requestBody =
                objectMapper.writeValueAsBytes(request);

        long paymentCountBefore =
                paymentRepository.count();

        mockMvc.perform(
                        post("/api/payments")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/api/payments")
                                .header("Idempotency-Key", "   ")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        assertThat(paymentRepository.count())
                .isEqualTo(paymentCountBefore);
    }

}