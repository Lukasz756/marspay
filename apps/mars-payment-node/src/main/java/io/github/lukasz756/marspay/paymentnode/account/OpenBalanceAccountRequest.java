package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OpenBalanceAccountRequest(
        @NotBlank
        @Pattern(
                regexp = "[A-Za-z]{3}",
                message = "Currency must contain exactly three letters"
        )
        String currency
) {
}