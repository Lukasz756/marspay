package io.github.lukasz756.marspay.relay.message;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelayMessageTest {

    @Test
    void createsPendingMessage() {
        RelayMessage message = pendingMessage();

        assertThat(message.getStatus())
                .isEqualTo(RelayMessageStatus.PENDING);

        assertThat(message.getAttemptCount()).isZero();
        assertThat(message.getDeliveredAt()).isNull();
        assertThat(message.getLastError()).isNull();
    }

    @Test
    void marksPendingMessageAsDelivered() {
        RelayMessage message = pendingMessage();
        Instant deliveredAt = Instant.parse("2035-01-10T12:00:00Z");

        message.markDelivered(deliveredAt);

        assertThat(message.getStatus())
                .isEqualTo(RelayMessageStatus.DELIVERED);

        assertThat(message.getAttemptCount()).isEqualTo(1);
        assertThat(message.getDeliveredAt()).isEqualTo(deliveredAt);
        assertThat(message.getLastError()).isNull();
    }

    @Test
    void schedulesAnotherAttemptWhenDeliveryFails() {
        RelayMessage message = pendingMessage();
        Instant nextAttemptAt = Instant.parse("2035-01-10T12:00:30Z");

        message.recordFailedAttempt(
                "  connection timeout  ",
                nextAttemptAt,
                5
        );

        assertThat(message.getStatus())
                .isEqualTo(RelayMessageStatus.PENDING);

        assertThat(message.getAttemptCount()).isEqualTo(1);
        assertThat(message.getAvailableAt()).isEqualTo(nextAttemptAt);
        assertThat(message.getLastError())
                .isEqualTo("connection timeout");

        assertThat(message.getDeliveredAt()).isNull();
    }

    @Test
    void marksMessageAsFailedAfterMaximumNumberOfAttempts() {
        RelayMessage message = pendingMessage();
        Instant nextAttemptAt = Instant.parse("2035-01-10T12:00:30Z");

        for (int attempt = 0; attempt < 5; attempt++) {
            message.recordFailedAttempt(
                    "connection timeout",
                    nextAttemptAt,
                    5
            );
        }

        assertThat(message.getStatus())
                .isEqualTo(RelayMessageStatus.FAILED);

        assertThat(message.getAttemptCount()).isEqualTo(5);
        assertThat(message.getLastError())
                .isEqualTo("connection timeout");

        assertThat(message.getDeliveredAt()).isNull();
    }

    @Test
    void rejectsAnotherDeliveryAfterMessageWasDelivered() {
        RelayMessage message = pendingMessage();

        message.markDelivered(
                Instant.parse("2035-01-10T12:00:00Z")
        );

        assertThatThrownBy(() -> message.markDelivered(
                Instant.parse("2035-01-10T12:01:00Z")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Only pending relay messages can be delivered"
                );

        assertThat(message.getStatus())
                .isEqualTo(RelayMessageStatus.DELIVERED);

        assertThat(message.getAttemptCount()).isEqualTo(1);
    }

    private RelayMessage pendingMessage() {
        return RelayMessage.pending(
                UUID.randomUUID(),
                "MARS_PAYMENT_NODE",
                "EARTH_PAYMENT_SERVICE",
                "PAYMENT",
                UUID.randomUUID(),
                "PAYMENT_CREATED",
                """
                {
                  "schemaVersion": 1
                }
                """,
                Instant.parse("2035-01-10T12:00:00Z")
        );
    }
}