package io.github.lukasz756.marspay.paymentnode.account;

import java.util.UUID;

public class BalanceAccountNotFoundException extends RuntimeException {
    public BalanceAccountNotFoundException(UUID balanceAccountId) {
        super(
                "Balance account with id '%s' not found".formatted(balanceAccountId)
        );
    }
}
