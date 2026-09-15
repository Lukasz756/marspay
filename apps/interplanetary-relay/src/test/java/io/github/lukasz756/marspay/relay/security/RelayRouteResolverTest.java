package io.github.lukasz756.marspay.relay.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelayRouteResolverTest {

    private final RelayRouteResolver resolver =
            new RelayRouteResolver();

    @Test
    void resolvesMarsRoute() {
        RelayRoute route =
                resolver.resolve("mars-payment-node");

        assertThat(route).isEqualTo(
                new RelayRoute(
                        "MARS_PAYMENT_NODE",
                        "EARTH_PAYMENT_SERVICE"
                )
        );
    }

    @Test
    void resolvesEarthRoute() {
        RelayRoute route =
                resolver.resolve("earth-payment-service");

        assertThat(route).isEqualTo(
                new RelayRoute(
                        "EARTH_PAYMENT_SERVICE",
                        "MARS_PAYMENT_NODE"
                )
        );
    }

    @Test
    void rejectsUnknownClient() {
        assertThatThrownBy(() ->
                                   resolver.resolve("unknown-client")
        )
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsMissingClientId() {
        assertThatThrownBy(() ->
                                   resolver.resolve(null)
        )
                .isInstanceOf(AccessDeniedException.class);

        assertThatThrownBy(() ->
                                   resolver.resolve(" ")
        )
                .isInstanceOf(AccessDeniedException.class);
    }
}