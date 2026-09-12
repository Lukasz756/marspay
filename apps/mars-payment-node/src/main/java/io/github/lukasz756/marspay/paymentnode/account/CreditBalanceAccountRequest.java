package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreditBalanceAccountRequest(@Positive long amountMinor,

                                          @NotBlank @Size(max = 100) String reference) {
}
