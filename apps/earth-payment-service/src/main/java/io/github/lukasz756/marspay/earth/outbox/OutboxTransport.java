package io.github.lukasz756.marspay.earth.outbox;


public interface OutboxTransport {

    void publish(OutboxMessage message);
}