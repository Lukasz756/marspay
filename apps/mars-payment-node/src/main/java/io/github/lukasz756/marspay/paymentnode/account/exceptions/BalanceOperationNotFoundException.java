package io.github.lukasz756.marspay.paymentnode.account.exceptions;

import java.util.UUID;

public class BalanceOperationNotFoundException extends RuntimeException {

    public BalanceOperationNotFoundException(UUID operationId) {
        super(
                "Balance operation with id '%s' not found"
                        .formatted(operationId)
        );
    }
}