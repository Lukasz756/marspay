package io.github.lukasz756.marspay.relay.delivery;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "marspay.relay")
public record RelayProperties(Duration deliveryDelay, List<Destination> destinations) {

    public RelayProperties {
        if (destinations == null || destinations.isEmpty()) {
            throw new IllegalArgumentException("Relay destinations must not be empty");
        }

        if (deliveryDelay == null || deliveryDelay.isZero() || deliveryDelay.isNegative()) {
            throw new IllegalArgumentException("Relay delivery delay must be greater than zero");
        }

        destinations = List.copyOf(destinations);
    }

    public URI destinationUrl(String destinationName) {
        if (destinationName == null || destinationName.isBlank()) {
            throw new IllegalArgumentException("Relay destination name must not be blank");
        }

        return destinations.stream()
                .filter(destination -> destination.name()
                        .equals(destinationName))
                .map(Destination::url)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown relay destination: " + destinationName));
    }

    public record Destination(String name, URI url) {
        public Destination {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Relay destination name must not be blank");
            }

            if (url == null) {
                throw new IllegalArgumentException("Relay destination URL must not be null");
            }
        }
    }
}