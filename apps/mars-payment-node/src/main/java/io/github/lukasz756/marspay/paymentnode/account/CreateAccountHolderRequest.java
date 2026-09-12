package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountHolderRequest(@NotBlank @Size(max = 100) String reference,

                                         @NotNull AccountHolderType type) {
}