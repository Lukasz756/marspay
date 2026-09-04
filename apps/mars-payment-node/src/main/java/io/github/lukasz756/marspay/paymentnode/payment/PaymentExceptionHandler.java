package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.payment.exceptions.*;
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

    @ExceptionHandler(BalanceTransferNotFoundException.class)
    ProblemDetail handleBalanceTransferNotFound(
            BalanceTransferNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("Balance transfer not found");

        return problem;
    }

    @ExceptionHandler(BalanceTransferSameAccountException.class)
    ProblemDetail handleBalanceTransferSameAccount(
            BalanceTransferSameAccountException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Source and target accounts are the same");

        return problem;
    }

    @ExceptionHandler(PaymentAlreadyExistsException.class)
    ProblemDetail handlePaymentAlreadyExistsException(
            PaymentAlreadyExistsException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("Payment already exists");

        return problem;
    }

    @ExceptionHandler(PaymentCurrencyMismatchException.class)
    ProblemDetail handlePaymentCurrencyMismatchException(
            PaymentCurrencyMismatchException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("Source and target accounts have different currency");

        return problem;
    }

    @ExceptionHandler(PaymentSameAccountException.class)
    ProblemDetail handlePaymentSameAccount(
            PaymentSameAccountException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Source and target accounts are the same");

        return problem;
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    ProblemDetail handlePaymentNotFound(PaymentNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("Payment not found");

        return problem;
    }

    @ExceptionHandler(PaymentInvalidStatusException.class)
    ProblemDetail handlePaymentInvalidStatus(
            PaymentInvalidStatusException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Invalid payment status");

        return problem;
    }
}