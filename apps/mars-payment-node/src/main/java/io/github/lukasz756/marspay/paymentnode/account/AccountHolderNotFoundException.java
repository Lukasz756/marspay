package io.github.lukasz756.marspay.paymentnode.account;

import java.util.UUID;

public class AccountHolderNotFoundException extends RuntimeException {

    public AccountHolderNotFoundException(UUID id) {
        super("Account holder with id '%s' was not found".formatted(id));
    }
}
