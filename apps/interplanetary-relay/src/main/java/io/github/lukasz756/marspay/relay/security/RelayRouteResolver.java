package io.github.lukasz756.marspay.relay.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class RelayRouteResolver {

    public RelayRoute resolve(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw accessDenied();
        }

        return switch (clientId) {
            case "mars-payment-node" ->
                    new RelayRoute(
                            "MARS_PAYMENT_NODE",
                            "EARTH_PAYMENT_SERVICE"
                    );

            case "earth-payment-service" ->
                    new RelayRoute(
                            "EARTH_PAYMENT_SERVICE",
                            "MARS_PAYMENT_NODE"
                    );

            default -> throw accessDenied();
        };
    }

    private AccessDeniedException accessDenied() {
        return new AccessDeniedException(
                "Client cannot submit relay messages"
        );
    }
}