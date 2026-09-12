package io.github.lukasz756.marspay.paymentnode.account.exceptions;

import java.util.UUID;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID balanceAccountId, long availableAmountMinor, long requestedAmountMinor) {
        super("Insufficient balance on account %s: available=%d, requested=%d".formatted(balanceAccountId,
                                                                                         availableAmountMinor,
                                                                                         requestedAmountMinor));
    }
}
