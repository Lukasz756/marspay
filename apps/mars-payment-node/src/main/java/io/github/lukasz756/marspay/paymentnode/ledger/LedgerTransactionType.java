package io.github.lukasz756.marspay.paymentnode.ledger;

public enum LedgerTransactionType {
    PAYMENT_AUTHORIZE,
    PAYMENT_AUTHORIZATION_DECLINED,
    PAYMENT_CAPTURE,
    PAYMENT_CANCEL,
    PAYMENT_REFUND
}