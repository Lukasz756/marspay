package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.payment.exceptions.BalanceTransferAlreadyExistsException;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.BalanceTransferCurrencyMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(BalanceTransferAlreadyExistsException.class)
    ProblemDetail handleBalanceTransferAlreadyExists(
            BalanceTransferAlreadyExistsException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Balance transfer already exists");

        return problem;
    }

    @ExceptionHandler(BalanceTransferCurrencyMismatchException.class)
    ProblemDetail handleBalanceTransferCurrencyMismatch(
            BalanceTransferCurrencyMismatchException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Balance transfer currency mismatch");

        return problem;
    }
}