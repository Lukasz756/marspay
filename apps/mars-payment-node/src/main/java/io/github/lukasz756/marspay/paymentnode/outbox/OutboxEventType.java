package io.github.lukasz756.marspay.paymentnode.outbox;

public enum OutboxEventType {
    PAYMENT_CREATED,
    PAYMENT_AUTHORIZATION_REQUESTED,
    PAYMENT_CAPTURE_REQUESTED,
    PAYMENT_CANCEL_REQUESTED,
    PAYMENT_REFUND_REQUESTED
}
