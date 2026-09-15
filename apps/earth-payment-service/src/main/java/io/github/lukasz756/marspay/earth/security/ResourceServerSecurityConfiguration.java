package io.github.lukasz756.marspay.earth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class ResourceServerSecurityConfiguration {

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
                                "/internal/relay/events"
                        )
                        .hasAuthority("SCOPE_relay.deliver")

                        .anyRequest()
                        .denyAll()
                )
                .oauth2ResourceServer(resourceServer ->
                                              resourceServer.jwt(Customizer.withDefaults())
                )
                .build();
    }
}