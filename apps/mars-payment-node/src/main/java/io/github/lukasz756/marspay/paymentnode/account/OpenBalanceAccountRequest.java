package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OpenBalanceAccountRequest(
        @NotBlank
        @Pattern(
                regexp = "(?i)MCR",
                message = "Currency must be MCR"
        )
        String currency
) {
}