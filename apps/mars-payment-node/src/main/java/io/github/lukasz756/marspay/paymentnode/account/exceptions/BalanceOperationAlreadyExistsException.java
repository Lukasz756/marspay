package io.github.lukasz756.marspay.paymentnode.account.exceptions;

import java.util.UUID;

public class BalanceOperationAlreadyExistsException
        extends RuntimeException {

    public BalanceOperationAlreadyExistsException(
            UUID balanceAccountId,
            String reference
    ) {
        super(
                "Balance operation with reference '%s' already exists for account '%s'"
                        .formatted(reference, balanceAccountId)
        );
    }
}