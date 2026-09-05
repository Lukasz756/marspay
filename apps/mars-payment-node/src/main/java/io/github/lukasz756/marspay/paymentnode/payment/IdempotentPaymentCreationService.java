package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.idempotency.IdempotencyResult;
import io.github.lukasz756.marspay.paymentnode.idempotency.IdempotencyScope;
import io.github.lukasz756.marspay.paymentnode.idempotency.IdempotencyService;
import io.github.lukasz756.marspay.paymentnode.idempotency.RequestHasher;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;

@Service
public class IdempotentPaymentCreationService {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotentPaymentCreationService(
            PaymentService paymentService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper
    ) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    public IdempotencyResult createPayment(
            String idempotencyKey,
            CreatePaymentRequest request
    ) {
        String requestHash = RequestHasher.sha256(
                canonicalRequest(request)
        );

        try {
            return idempotencyService.execute(
                    IdempotencyScope.CREATE_PAYMENT,
                    idempotencyKey,
                    requestHash,
                    () -> executePaymentCreation(request)
            );
        } catch (DataIntegrityViolationException exception) {
            if (!isIdempotencyKeyConflict(exception)) {
                throw exception;
            }

            return idempotencyService.replayExisting(
                    IdempotencyScope.CREATE_PAYMENT,
                    idempotencyKey,
                    requestHash
            );
        }
    }

    private IdempotencyResult executePaymentCreation(
            CreatePaymentRequest request
    ) {
        Payment payment = paymentService.createPayment(
                request.sourceBalanceAccountId(),
                request.targetBalanceAccountId(),
                request.amountMinor(),
                request.reference()
        );

        PaymentResponse response = PaymentResponse.from(payment);

        String location = URI.create(
                "/api/payments/" + response.id()
        ).toString();

        try {
            String responseBody =
                    objectMapper.writeValueAsString(response);

            return IdempotencyResult.firstExecution(
                    201,
                    responseBody,
                    location
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Cannot serialize payment response",
                    exception
            );
        }
    }

    private String canonicalRequest(
            CreatePaymentRequest request
    ) {
        return "source=" + request.sourceBalanceAccountId()
                + "\ntarget=" + request.targetBalanceAccountId()
                + "\namount=" + request.amountMinor()
                + "\nreference=" + request.reference().trim();
    }

    private boolean isIdempotencyKeyConflict(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && "uq_idempotency_record_scope_key".equals(
                    violation.getConstraintName()
            )) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}