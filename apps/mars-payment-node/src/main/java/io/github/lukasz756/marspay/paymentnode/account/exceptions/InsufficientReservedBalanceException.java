package io.github.lukasz756.marspay.paymentnode.account.exceptions;

import java.util.UUID;

public class InsufficientReservedBalanceException extends RuntimeException {

    public InsufficientReservedBalanceException(
            UUID balanceAccountId,
            long reservedBalanceMinor,
            long requestedAmountMinor
    ) {
        super(
                "Insufficient reserved balance for account " + balanceAccountId
                        + ": reserved " + reservedBalanceMinor
                        + ", requested " + requestedAmountMinor
        );
    }
}