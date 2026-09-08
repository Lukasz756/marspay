package io.github.lukasz756.marspay.paymentnode.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface InboxEventRepository
        extends JpaRepository<InboxEvent, UUID> {

    List<InboxEvent>
    findTop50ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscReceivedAtAsc(
            InboxEventStatus status,
            Instant availableAt
    );
}