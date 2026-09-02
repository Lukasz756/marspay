package io.github.lukasz756.marspay.paymentnode.account.exceptions;

import java.util.UUID;

public class BalanceAccountAlreadyExistsException extends RuntimeException {

    public BalanceAccountAlreadyExistsException(
            UUID accountHolderId,
            String currency
    ) {
        super(
                "Balance account for account holder '%s' and currency '%s' already exists"
                        .formatted(accountHolderId, currency)
        );
    }
}