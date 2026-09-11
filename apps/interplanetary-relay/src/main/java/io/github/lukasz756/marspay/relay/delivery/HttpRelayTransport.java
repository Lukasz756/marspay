package io.github.lukasz756.marspay.relay.delivery;

import io.github.lukasz756.marspay.relay.message.RelayDeliveryMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.UUID;

@Component
public class HttpRelayTransport implements RelayTransport {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RelayProperties relayProperties;

    public HttpRelayTransport(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            RelayProperties relayProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.relayProperties = relayProperties;
    }

    @Override
    public void deliver(RelayDeliveryMessage message) {
        URI destinationUrl =
                relayProperties.destinationUrl(
                        message.destination()
                );

        DeliveryRequest request = new DeliveryRequest(
                message.eventId(),
                message.source(),
                message.aggregateType(),
                message.aggregateId(),
                message.eventType(),
                deserializePayload(message)
        );

        restClient.post()
                .uri(destinationUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private JsonNode deserializePayload(
            RelayDeliveryMessage message
    ) {
        try {
            return objectMapper.readTree(
                    message.payloadJson()
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Cannot deserialize relay message payload: "
                            + message.eventId(),
                    exception
            );
        }
    }

    private record DeliveryRequest(
            UUID eventId,
            String source,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            JsonNode payload
    ) {
    }
}