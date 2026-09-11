package io.github.lukasz756.marspay.relay.message;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/relay/messages")
public class RelayMessageController {

    private final RelayMessageService relayMessageService;

    RelayMessageController(
            RelayMessageService relayMessageService
    ) {
        this.relayMessageService = relayMessageService;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @Valid @RequestBody RelayMessageRequest request
    ) {
        RelayReceiveResult result =
                relayMessageService.receive(request);

        return ResponseEntity
                .accepted()
                .header(
                        "Relay-Duplicate",
                        Boolean.toString(result.duplicate())
                )
                .build();
    }
}