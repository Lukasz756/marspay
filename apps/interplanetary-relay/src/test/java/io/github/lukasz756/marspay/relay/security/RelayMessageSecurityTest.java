package io.github.lukasz756.marspay.relay.security;

import io.github.lukasz756.marspay.relay.delivery.RelayProperties;
import io.github.lukasz756.marspay.relay.message.RelayMessageController;
import io.github.lukasz756.marspay.relay.message.RelayMessageRequest;
import io.github.lukasz756.marspay.relay.message.RelayMessageService;
import io.github.lukasz756.marspay.relay.message.RelayReceiveResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RelayMessageController.class)
@Import({
        SecurityConfiguration.class,
        RelayRouteResolver.class
})
class RelayMessageSecurityTest {

    private static final String ENDPOINT =
            "/api/relay/messages";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RelayMessageService relayMessageService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RelayProperties relayProperties;

    @Test
    void rejectsRequestWithoutJwt() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(relayMessageService);
    }

    @Test
    void rejectsJwtWithoutSubmitScope() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                                .with(token(
                                        "mars-payment-node",
                                        "SCOPE_relay.deliver"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(relayMessageService);
    }

    @Test
    void acceptsMarsClientAndUsesResolvedRoute()
            throws Exception {
        UUID eventId = UUID.randomUUID();

        when(relayMessageService.receive(
                eq("MARS_PAYMENT_NODE"),
                eq("EARTH_PAYMENT_SERVICE"),
                any(RelayMessageRequest.class)
        )).thenReturn(
                new RelayReceiveResult(eventId, false)
        );

        mockMvc.perform(post(ENDPOINT)
                                .with(token(
                                        "mars-payment-node",
                                        "SCOPE_relay.submit"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(eventId)))
                .andExpect(status().isAccepted())
                .andExpect(header().string(
                        "Relay-Duplicate",
                        "false"
                ));

        verify(relayMessageService).receive(
                eq("MARS_PAYMENT_NODE"),
                eq("EARTH_PAYMENT_SERVICE"),
                any(RelayMessageRequest.class)
        );
    }

    @Test
    void acceptsEarthClientAndUsesResolvedRoute()
            throws Exception {
        UUID eventId = UUID.randomUUID();

        when(relayMessageService.receive(
                eq("EARTH_PAYMENT_SERVICE"),
                eq("MARS_PAYMENT_NODE"),
                any(RelayMessageRequest.class)
        )).thenReturn(
                new RelayReceiveResult(eventId, false)
        );

        mockMvc.perform(post(ENDPOINT)
                                .with(token(
                                        "earth-payment-service",
                                        "SCOPE_relay.submit"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(eventId)))
                .andExpect(status().isAccepted());

        verify(relayMessageService).receive(
                eq("EARTH_PAYMENT_SERVICE"),
                eq("MARS_PAYMENT_NODE"),
                any(RelayMessageRequest.class)
        );
    }

    @Test
    void rejectsUnknownClient() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                                .with(token(
                                        "unknown-client",
                                        "SCOPE_relay.submit"
                                ))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody(UUID.randomUUID())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(relayMessageService);
    }

    private RequestPostProcessor token(
            String subject,
            String authority
    ) {
        return jwt()
                .jwt(jwt -> jwt.subject(subject))
                .authorities(
                        new SimpleGrantedAuthority(authority)
                );
    }

    private byte[] requestBody(UUID eventId)
            throws Exception {
        RelayMessageRequest request =
                new RelayMessageRequest(
                        eventId,
                        "PAYMENT",
                        UUID.randomUUID(),
                        "PAYMENT_CREATED",
                        objectMapper.readTree("""
                                {
                                  "schemaVersion": 1,
                                  "status": "CREATED"
                                }
                                """)
                );

        return objectMapper.writeValueAsBytes(request);
    }
}