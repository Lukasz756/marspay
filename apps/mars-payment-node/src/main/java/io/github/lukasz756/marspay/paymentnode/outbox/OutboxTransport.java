package io.github.lukasz756.marspay.paymentnode.outbox;

public interface OutboxTransport {

    void publish(OutboxMessage message);
}