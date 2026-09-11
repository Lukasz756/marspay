package io.github.lukasz756.marspay.relay.delivery;

import io.github.lukasz756.marspay.relay.message.RelayDeliveryMessage;

public interface RelayTransport {

    void deliver(RelayDeliveryMessage message);
}