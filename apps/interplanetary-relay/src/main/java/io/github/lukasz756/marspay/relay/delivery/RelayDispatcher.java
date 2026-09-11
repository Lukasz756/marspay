package io.github.lukasz756.marspay.relay.delivery;

import io.github.lukasz756.marspay.relay.message.RelayDeliveryMessage;
import io.github.lukasz756.marspay.relay.message.RelayMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class RelayDispatcher {

    private static final Logger logger =
            LoggerFactory.getLogger(RelayDispatcher.class);

    private static final int MAX_ATTEMPTS = 5;

    private static final Duration RETRY_DELAY =
            Duration.ofSeconds(30);

    private final RelayMessageService relayMessageService;
    private final RelayTransport relayTransport;

    public RelayDispatcher(
            RelayMessageService relayMessageService,
            RelayTransport relayTransport
    ) {
        this.relayMessageService = relayMessageService;
        this.relayTransport = relayTransport;
    }

    @Scheduled(fixedDelay = 5000)
    public void dispatchReadyMessages() {
        List<RelayDeliveryMessage> messages =
                relayMessageService.findReadyMessages(
                        Instant.now()
                );

        for (RelayDeliveryMessage message : messages) {
            dispatch(message);
        }
    }

    private void dispatch(RelayDeliveryMessage message) {
        try {
            relayTransport.deliver(message);
        } catch (RuntimeException exception) {
            logger.warn(
                    "Failed to deliver relay message: eventId={}",
                    message.eventId(),
                    exception
            );

            relayMessageService.recordFailedAttempt(
                    message.eventId(),
                    errorMessage(exception),
                    Instant.now().plus(RETRY_DELAY),
                    MAX_ATTEMPTS
            );

            return;
        }

        relayMessageService.markDelivered(
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