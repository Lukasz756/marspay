package io.github.lukasz756.marspay.earth.inbox;

import java.util.UUID;

public record InboxReceiveResult(
        UUID eventId,
        boolean duplicate
) {
}