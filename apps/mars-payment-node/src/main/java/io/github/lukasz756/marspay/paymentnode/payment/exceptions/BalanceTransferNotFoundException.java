package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import java.util.UUID;

public class BalanceTransferNotFoundException extends RuntimeException {

    public BalanceTransferNotFoundException(UUID transferId) {
        super("Balance transfer with id '%s' not found".formatted(transferId));
    }
}