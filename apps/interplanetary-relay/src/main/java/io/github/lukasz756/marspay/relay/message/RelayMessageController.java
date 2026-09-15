package io.github.lukasz756.marspay.relay.message;

import io.github.lukasz756.marspay.relay.security.RelayRoute;
import io.github.lukasz756.marspay.relay.security.RelayRouteResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relay/messages")
public class RelayMessageController {

    private final RelayMessageService relayMessageService;
    private final RelayRouteResolver relayRouteResolver;

    RelayMessageController(
            RelayMessageService relayMessageService,
            RelayRouteResolver relayRouteResolver
    ) {
        this.relayMessageService = relayMessageService;
        this.relayRouteResolver = relayRouteResolver;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RelayMessageRequest request
    ) {
        RelayRoute route =
                relayRouteResolver.resolve(jwt.getSubject());

        RelayReceiveResult result =
                relayMessageService.receive(
                        route.source(),
                        route.destination(),
                        request
                );

        return ResponseEntity.accepted()
                .header(
                        "Relay-Duplicate",
                        Boolean.toString(result.duplicate())
                )
                .build();
    }
}