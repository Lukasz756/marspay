package io.github.lukasz756.marspay.paymentnode.payment;

import java.time.Instant;
import java.util.UUID;

public record BalanceTransferResponse(
        UUID id,
        UUID sourceBalanceAccountId,
        UUID targetBalanceAccountId,
        long amountMinor,
        String currency,
        String reference,
        Instant createdAt
) {

    public static BalanceTransferResponse from(
            BalanceTransfer transfer
    ) {
        return new BalanceTransferResponse(
                transfer.getId(),
                transfer.getSourceBalanceAccountId(),
                transfer.getTargetBalanceAccountId(),
                transfer.getAmountMinor(),
                transfer.getCurrency(),
                transfer.getReference(),
                transfer.getCreatedAt()
        );
    }
}