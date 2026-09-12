package io.github.lukasz756.marspay.relay.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface RelayMessageRepository extends JpaRepository<RelayMessage, UUID> {

    List<RelayMessage> findTop50ByStatusAndAvailableAtLessThanEqualOrderByAvailableAtAscReceivedAtAsc(RelayMessageStatus status, Instant availableAt);
}