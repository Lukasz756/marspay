package io.github.lukasz756.marspay.paymentnode.payment.exceptions;

import java.util.UUID;

public class PaymentSameAccountException extends RuntimeException {

    public PaymentSameAccountException(UUID balanceAccountId) {
        super(
                "Source and target balance accounts must be different: "
                        + balanceAccountId
        );
    }
}