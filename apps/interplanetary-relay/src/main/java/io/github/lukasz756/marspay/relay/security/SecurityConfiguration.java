package io.github.lukasz756.marspay.relay.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/actuator/health/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/relay/messages"
                        )
                        .hasAuthority("SCOPE_relay.submit")
                        .anyRequest()
                        .denyAll()
                )
                .oauth2ResourceServer(resourceServer ->
                                              resourceServer.jwt(Customizer.withDefaults())
                )
                .build();
    }
}