package io.github.lukasz756.marspay.paymentnode.inbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class InboxProcessor {

    private static final Logger logger =
            LoggerFactory.getLogger(InboxProcessor.class);

    private static final int MAX_ATTEMPTS = 5;

    private static final Duration RETRY_DELAY =
            Duration.ofSeconds(30);

    private final InboxService inboxService;

    public InboxProcessor(InboxService inboxService) {
        this.inboxService = inboxService;
    }

    @Scheduled(
            fixedDelayString =
                    "${marspay.inbox.processor.fixed-delay-ms:5000}"
    )
    public void processReadyEvents() {
        List<UUID> eventIds =
                inboxService.findReadyEventIds(Instant.now());

        for (UUID eventId : eventIds) {
            process(eventId);
        }
    }

    private void process(UUID eventId) {
        try {
            inboxService.processEvent(
                    eventId,
                    Instant.now()
            );
        } catch (RuntimeException exception) {
            logger.warn(
                    "Failed to process inbox event: eventId={}",
                    eventId,
                    exception
            );

            inboxService.recordFailedAttempt(
                    eventId,
                    errorMessage(exception),
                    Instant.now().plus(RETRY_DELAY),
                    MAX_ATTEMPTS
            );
        }
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return exception.getClass().getSimpleName()
                + ": "
                + message;
    }
}