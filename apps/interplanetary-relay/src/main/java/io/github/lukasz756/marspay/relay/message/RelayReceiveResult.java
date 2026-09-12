package io.github.lukasz756.marspay.relay.message;

import java.util.UUID;

public record RelayReceiveResult(UUID eventId, boolean duplicate) {
}