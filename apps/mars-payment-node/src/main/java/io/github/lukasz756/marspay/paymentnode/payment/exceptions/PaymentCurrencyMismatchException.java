package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import java.util.UUID;

public class PaymentCurrencyMismatchException extends RuntimeException {

    public PaymentCurrencyMismatchException(UUID sourceBalanceAccountId, String sourceCurrency,
                                            UUID targetBalanceAccountId, String targetCurrency) {
        super("Cannot create payment from account " + sourceBalanceAccountId + " (" + sourceCurrency + ") to account "
                      + targetBalanceAccountId + " (" + targetCurrency + ")");
    }
}