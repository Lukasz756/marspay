package io.github.lukasz756.marspay.paymentnode.inbox;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record IncomingEventRequest(@NotNull UUID eventId,

                                   @NotBlank @Size(max = 50) String source,

                                   @NotBlank @Size(max = 50) String aggregateType,

                                   @NotNull UUID aggregateId,

                                   @NotBlank @Size(max = 50) String eventType,

                                   @NotNull JsonNode payload) {
}