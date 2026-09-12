package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import java.util.UUID;

public class BalanceTransferAlreadyExistsException extends RuntimeException {

    public BalanceTransferAlreadyExistsException(UUID sourceBalanceAccountId, String reference) {
        super("Transfer from account %s with reference %s already exists".formatted(sourceBalanceAccountId, reference));
    }
}
