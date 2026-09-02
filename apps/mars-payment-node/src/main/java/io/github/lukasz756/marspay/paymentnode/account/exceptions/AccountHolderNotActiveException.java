package io.github.lukasz756.marspay.paymentnode.account.exceptions;

import java.util.UUID;

public class AccountHolderNotActiveException extends RuntimeException {
    public AccountHolderNotActiveException(UUID id) {
        super(
                "Account holder with id '%s' does not have the ACTIVE status".formatted(id)
        );
    }
}
