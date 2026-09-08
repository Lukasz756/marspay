package io.github.lukasz756.marspay.paymentnode.payment;

public enum PaymentOperationType {
    CREATE,

    AUTHORIZE,
    AUTHORIZATION_REQUESTED,
    AUTHORIZATION_CONFIRMED,
    AUTHORIZATION_DECLINED,

    CAPTURE,
    CANCEL,
    REFUND
}