package io.github.lukasz756.marspay.earth.outbox;


import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent>
    findTop50ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscCreatedAtAsc(
            OutboxEventStatus status,
            Instant availableAt
    );
}