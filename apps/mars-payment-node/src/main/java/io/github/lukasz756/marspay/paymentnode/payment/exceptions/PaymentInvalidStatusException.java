package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import io.github.lukasz756.marspay.paymentnode.payment.PaymentStatus;

import java.util.Arrays;
import java.util.UUID;

public class PaymentInvalidStatusException extends RuntimeException {

    public PaymentInvalidStatusException(
            UUID paymentId,
            PaymentStatus actualStatus,
            PaymentStatus... expectedStatuses
    ) {
        super(
                "Payment " + paymentId
                        + " has status " + actualStatus
                        + "; expected one of "
                        + Arrays.toString(expectedStatuses)
        );
    }
}