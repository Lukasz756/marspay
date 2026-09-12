package io.github.lukasz756.marspay.paymentnode.payment;

public enum PaymentStatus {
    CREATED,

    AUTHORIZATION_PENDING, AUTHORIZED, DECLINED,

    CAPTURE_PENDING, CAPTURED,

    CANCEL_PENDING, CANCELLED,

    REFUND_PENDING, REFUNDED
}