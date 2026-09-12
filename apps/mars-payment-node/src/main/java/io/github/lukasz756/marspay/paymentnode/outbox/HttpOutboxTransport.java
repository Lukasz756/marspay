package io.github.lukasz756.marspay.paymentnode.outbox;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.UUID;

@Component
@Profile("local")
public class HttpOutboxTransport implements OutboxTransport {

    private static final URI RELAY_URL = URI.create("http://localhost:8081/api/relay/messages");

    private static final String SOURCE = "MARS_PAYMENT_NODE";

    private static final String DESTINATION = "EARTH_PAYMENT_SERVICE";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpOutboxTransport(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(OutboxMessage message) {
        RelayRequest request = new RelayRequest(message.eventId(), SOURCE, DESTINATION,
                                                message.aggregateType()
                                                        .name(), message.aggregateId(), message.eventType()
                                                        .name(),
                                                deserializePayload(message));

        restClient.post()
                .uri(RELAY_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private JsonNode deserializePayload(OutboxMessage message) {
        try {
            return objectMapper.readTree(message.payloadJson());
        } catch (JacksonException exception) {
            throw new IllegalStateException("Cannot deserialize outbox payload: " + message.eventId(), exception);
        }
    }

    private record RelayRequest(UUID eventId, String source, String destination, String aggregateType, UUID aggregateId,
                                String eventType, JsonNode payload) {
    }
}