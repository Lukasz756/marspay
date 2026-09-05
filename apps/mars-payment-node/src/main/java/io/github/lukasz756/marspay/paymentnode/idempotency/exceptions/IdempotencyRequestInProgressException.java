package io.github.lukasz756.marspay.paymentnode.idempotency.exceptions;

public class IdempotencyRequestInProgressException
        extends RuntimeException {

    public IdempotencyRequestInProgressException(String idempotencyKey) {
        super(
                "Request with idempotency key '" + idempotencyKey
                        + "' is still being processed"
        );
    }
}