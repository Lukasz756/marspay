package io.github.lukasz756.marspay.paymentnode.idempotency;

import io.github.lukasz756.marspay.paymentnode.idempotency.exceptions.IdempotencyKeyConflictException;
import io.github.lukasz756.marspay.paymentnode.idempotency.exceptions.IdempotencyRequestInProgressException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IdempotencyResult execute(IdempotencyScope scope, String idempotencyKey, String requestHash,
                                     Supplier<IdempotencyResult> action) {
        String normalizedKey = normalizeKey(idempotencyKey);

        Optional<IdempotencyRecord> existingRecord = repository.findByScopeAndIdempotencyKey(scope, normalizedKey);

        if (existingRecord.isPresent()) {
            return replay(existingRecord.get(), normalizedKey, requestHash);
        }

        IdempotencyRecord record = IdempotencyRecord.start(scope, normalizedKey, requestHash);

        repository.saveAndFlush(record);

        IdempotencyResult result = action.get();

        record.complete(result.responseStatus(), result.responseBody(), result.responseLocation());

        return result;
    }

    @Transactional(readOnly = true)
    public IdempotencyResult replayExisting(IdempotencyScope scope, String idempotencyKey, String requestHash) {
        String normalizedKey = normalizeKey(idempotencyKey);

        IdempotencyRecord record =
                repository.findByScopeAndIdempotencyKey(scope, normalizedKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "Idempotency record was not found after " + "a unique constraint conflict"));

        return replay(record, normalizedKey, requestHash);
    }

    private IdempotencyResult replay(IdempotencyRecord record, String idempotencyKey, String requestHash) {
        if (!record.getRequestHash()
                .equals(requestHash)) {
            throw new IdempotencyKeyConflictException(idempotencyKey);
        }

        if (record.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            throw new IdempotencyRequestInProgressException(idempotencyKey);
        }

        return IdempotencyResult.replay(record);
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key must not be blank");
        }

        String normalizedKey = idempotencyKey.trim();

        if (normalizedKey.length() > 255) {
            throw new IllegalArgumentException("Idempotency key must be at most 255 characters");
        }

        return normalizedKey;
    }
}