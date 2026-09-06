package io.github.lukasz756.marspay.paymentnode.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Profile("local")
public class OutboxPublisher {

    private static final Logger logger =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration RETRY_DELAY =
            Duration.ofSeconds(30);

    private final OutboxService outboxService;
    private final OutboxTransport outboxTransport;

    public OutboxPublisher(
            OutboxService outboxService,
            OutboxTransport outboxTransport
    ) {
        this.outboxService = outboxService;
        this.outboxTransport = outboxTransport;
    }

    @Scheduled(
            fixedDelayString =
                    "${marspay.outbox.publisher.fixed-delay-ms:5000}"
    )
    public void publishReadyEvents() {
        List<OutboxMessage> messages =
                outboxService.findReadyEvents(Instant.now());

        for (OutboxMessage message : messages) {
            publish(message);
        }
    }

    private void publish(OutboxMessage message) {
        try {
            outboxTransport.publish(message);
        } catch (RuntimeException exception) {
            logger.warn(
                    "Failed to publish outbox event: eventId={}",
                    message.eventId(),
                    exception
            );

            outboxService.recordFailedAttempt(
                    message.eventId(),
                    errorMessage(exception),
                    Instant.now().plus(RETRY_DELAY),
                    MAX_ATTEMPTS
            );

            return;
        }

        outboxService.markPublished(
                message.eventId(),
                Instant.now()
        );
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