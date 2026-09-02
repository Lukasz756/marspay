package io.github.lukasz756.marspay.paymentnode.account.exceptions;

public class AccountHolderReferenceAlreadyExistsException extends RuntimeException {
    public AccountHolderReferenceAlreadyExistsException(String reference) {
        super("Account holder with reference '%s' already exists".formatted(reference));
    }
}
