package io.github.lukasz756.marspay.authorizationserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.util.List;

@Configuration
class JwtClaimsConfiguration {

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(
                    context.getTokenType()
            )) {
                return;
            }

            String clientId = context.getRegisteredClient()
                    .getClientId();

            switch (clientId) {
                case "mars-payment-node",
                     "earth-payment-service" ->
                        context.getClaims().audience(
                                List.of("interplanetary-relay")
                        );

                case "interplanetary-relay" ->
                        context.getClaims().audience(
                                List.of(
                                        "mars-payment-node",
                                        "earth-payment-service"
                                )
                        );
            }
        };
    }
}