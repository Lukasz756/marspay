package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import java.util.UUID;

public class BalanceTransferCurrencyMismatchException
        extends RuntimeException {

    public BalanceTransferCurrencyMismatchException(
            UUID sourceAccountId,
            String sourceCurrency,
            UUID targetAccountId,
            String targetCurrency
    ) {
        super(
                "Cannot transfer from account %s (%s) to account %s (%s)"
                        .formatted(
                                sourceAccountId,
                                sourceCurrency,
                                targetAccountId,
                                targetCurrency
                        )
        );
    }
}
