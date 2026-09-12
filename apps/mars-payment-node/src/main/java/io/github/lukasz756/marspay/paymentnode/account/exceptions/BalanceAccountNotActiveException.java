package io.github.lukasz756.marspay.paymentnode.account.exceptions;

import java.util.UUID;

public class BalanceAccountNotActiveException extends RuntimeException {

    public BalanceAccountNotActiveException(UUID balanceAccountId) {
        super("Balance account with id '%s' is not active".formatted(balanceAccountId));
    }
}