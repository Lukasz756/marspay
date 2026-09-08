package io.github.lukasz756.marspay.paymentnode.inbox;

import java.util.UUID;

public record InboxReceiveResult(
        UUID eventId,
        boolean duplicate
) {
}