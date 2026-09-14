package io.github.lukasz756.marspay.relay.delivery;

import io.github.lukasz756.marspay.relay.message.RelayDeliveryMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class HttpRelayTransportTest {

    private static final URI EARTH_URL = URI.create(
            "http://earth-payment-service.test/api/inbox/events"
    );

    private MockRestServiceServer mockServer;
    private HttpRelayTransport transport;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        RelayProperties relayProperties = new RelayProperties(
                Duration.ofSeconds(30),
                List.of(
                        new RelayProperties.Destination(
                                "EARTH_PAYMENT_SERVICE",
                                EARTH_URL
                        )
                )
        );

        transport = new HttpRelayTransport(
                restClientBuilder,
                new ObjectMapper(),
                relayProperties
        );
    }

    @Test
    void sendsMessageToConfiguredDestination() {
        UUID eventId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();

        RelayDeliveryMessage message = relayMessage(
                eventId,
                aggregateId,
                """
                {
                  "schemaVersion": 1,
                  "status": "CREATED"
                }
                """
        );

        String expectedRequest = """
                {
                  "eventId": "%s",
                  "source": "MARS_PAYMENT_NODE",
                  "aggregateType": "PAYMENT",
                  "aggregateId": "%s",
                  "eventType": "PAYMENT_CREATED",
                  "payload": {
                    "schemaVersion": 1,
                    "status": "CREATED"
                  }
                }
                """.formatted(eventId, aggregateId);

        mockServer.expect(requestTo(EARTH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(expectedRequest))
                .andRespond(withStatus(HttpStatus.ACCEPTED));

        transport.deliver(message);

        mockServer.verify();
    }

    @Test
    void propagatesServerError() {
        UUID eventId = UUID.randomUUID();

        RelayDeliveryMessage message = relayMessage(
                eventId,
                UUID.randomUUID(),
                """
                {
                  "schemaVersion": 1
                }
                """
        );

        mockServer.expect(requestTo(EARTH_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> transport.deliver(message))
                .isInstanceOf(RestClientResponseException.class);

        mockServer.verify();
    }

    @Test
    void rejectsMalformedPayloadBeforeSendingRequest() {
        UUID eventId = UUID.randomUUID();

        RelayDeliveryMessage message = relayMessage(
                eventId,
                UUID.randomUUID(),
                "{not-valid-json}"
        );

        assertThatThrownBy(() -> transport.deliver(message))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Cannot deserialize relay message payload: " + eventId
                );

        mockServer.verify();
    }

    private RelayDeliveryMessage relayMessage(
            UUID eventId,
            UUID aggregateId,
            String payloadJson
    ) {
        return new RelayDeliveryMessage(
                eventId,
                "MARS_PAYMENT_NODE",
                "EARTH_PAYMENT_SERVICE",
                "PAYMENT",
                aggregateId,
                "PAYMENT_CREATED",
                payloadJson,
                Instant.parse("2035-01-10T12:00:00Z")
        );
    }
}