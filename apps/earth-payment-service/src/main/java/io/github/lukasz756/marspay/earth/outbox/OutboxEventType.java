package io.github.lukasz756.marspay.earth.outbox;

public enum OutboxEventType {

    PAYMENT_AUTHORIZED("AUTHORIZED"), PAYMENT_DECLINED("DECLINED"), PAYMENT_CAPTURED("CAPTURED"),
    PAYMENT_CAPTURE_FAILED("CAPTURE_FAILED"), PAYMENT_CANCELLED("CANCELLED"), PAYMENT_CANCEL_FAILED("CANCEL_FAILED"),
    PAYMENT_REFUNDED("REFUNDED"), PAYMENT_REFUND_FAILED("REFUND_FAILED");

    private final String resultStatus;

    OutboxEventType(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String resultStatus() {
        return resultStatus;
    }
}