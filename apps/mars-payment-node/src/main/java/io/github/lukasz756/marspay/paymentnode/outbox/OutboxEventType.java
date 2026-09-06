package io.github.lukasz756.marspay.paymentnode.outbox;

public enum OutboxEventType {
    PAYMENT_CREATED,
    PAYMENT_AUTHORIZED,
    PAYMENT_CAPTURED,
    PAYMENT_CANCELLED,
    PAYMENT_REFUNDED
}