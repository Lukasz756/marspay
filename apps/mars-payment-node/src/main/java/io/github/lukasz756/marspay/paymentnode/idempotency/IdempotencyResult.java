package io.github.lukasz756.marspay.paymentnode.idempotency;

public record IdempotencyResult(
        int responseStatus,
        String responseBody,
        String responseLocation,
        boolean replayed
) {

    public static IdempotencyResult firstExecution(
            int responseStatus,
            String responseBody,
            String responseLocation
    ) {
        return new IdempotencyResult(
                responseStatus,
                responseBody,
                responseLocation,
                false
        );
    }

    public static IdempotencyResult replay(
            IdempotencyRecord record
    ) {
        if (record.getStatus() != IdempotencyStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Cannot replay an incomplete idempotency record"
            );
        }

        return new IdempotencyResult(
                record.getResponseStatus(),
                record.getResponseBody(),
                record.getResponseLocation(),
                true
        );
    }
}