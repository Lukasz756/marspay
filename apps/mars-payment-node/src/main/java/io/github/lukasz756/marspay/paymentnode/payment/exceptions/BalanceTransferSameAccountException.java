package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import java.util.UUID;

public class BalanceTransferSameAccountException extends RuntimeException {

    public BalanceTransferSameAccountException(UUID balanceAccountId) {
        super("Source and target balance account must be different: %s".formatted(balanceAccountId));
    }
}