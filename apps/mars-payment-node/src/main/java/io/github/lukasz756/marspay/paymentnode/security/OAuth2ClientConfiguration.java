package io.github.lukasz756.marspay.paymentnode.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
class OAuth2ClientConfiguration {

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients
    ) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                registrations,
                authorizedClients
        );
    }

    @Bean
    RestClient relayRestClient(
            RestClient.Builder builder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(
                        authorizedClientManager
                );

        interceptor.setClientRegistrationIdResolver(
                request -> "relay-client"
        );

        interceptor.setAuthorizationFailureHandler(
                OAuth2ClientHttpRequestInterceptor
                        .authorizationFailureHandler(
                                authorizedClientService
                        )
        );

        return builder
                .requestInterceptor(interceptor)
                .build();
    }
}