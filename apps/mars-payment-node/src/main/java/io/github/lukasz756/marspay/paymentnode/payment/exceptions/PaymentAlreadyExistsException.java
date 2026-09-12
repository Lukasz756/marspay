package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import java.util.UUID;

public class PaymentAlreadyExistsException extends RuntimeException {

    public PaymentAlreadyExistsException(UUID sourceBalanceAccountId, String reference) {
        super("Payment for source account " + sourceBalanceAccountId + " with reference '" + reference + "' already " +
                      "exists");
    }
}