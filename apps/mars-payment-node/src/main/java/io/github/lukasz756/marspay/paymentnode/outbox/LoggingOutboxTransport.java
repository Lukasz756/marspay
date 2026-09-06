package io.github.lukasz756.marspay.paymentnode.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LoggingOutboxTransport implements OutboxTransport {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingOutboxTransport.class);

    @Override
    public void publish(OutboxMessage message) {
        logger.info(
                "Simulated outbox publication: eventId={}, "
                        + "aggregateType={}, aggregateId={}, eventType={}",
                message.eventId(),
                message.aggregateType(),
                message.aggregateId(),
                message.eventType()
        );

        logger.debug(
                "Outbox event payload: {}",
                message.payloadJson()
        );
    }
}