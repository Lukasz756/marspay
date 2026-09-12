package io.github.lukasz756.marspay.paymentnode.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBalanceTransferRequest(

        @NotNull UUID sourceBalanceAccountId,

        @NotNull UUID targetBalanceAccountId,

        @Positive long amountMinor,

        @NotBlank @Size(max = 100) String reference) {
}