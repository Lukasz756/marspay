package io.github.lukasz756.marspay.paymentnode.idempotency.exceptions;

public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(String idempotencyKey) {
        super(
                "Idempotency key '" + idempotencyKey
                        + "' was already used with different request data"
        );
    }
}