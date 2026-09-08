package io.github.lukasz756.marspay.paymentnode.inbox;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/relay/events")
public class RelayEventController {

    private final InboxService inboxService;

    RelayEventController(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveEvent(
            @Valid @RequestBody IncomingEventRequest request
    ) {
        InboxReceiveResult result = inboxService.receive(request);

        return ResponseEntity
                .accepted()
                .header(
                        "Inbox-Duplicate",
                        Boolean.toString(result.duplicate())
                )
                .build();
    }
}