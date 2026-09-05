package io.github.lukasz756.marspay.paymentnode.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByScopeAndIdempotencyKey(
            IdempotencyScope scope,
            String idempotencyKey
    );
}