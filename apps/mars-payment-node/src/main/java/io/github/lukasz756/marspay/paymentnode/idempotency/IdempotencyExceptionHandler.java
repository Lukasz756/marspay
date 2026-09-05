package io.github.lukasz756.marspay.paymentnode.idempotency;

import io.github.lukasz756.marspay.paymentnode.idempotency.exceptions.IdempotencyKeyConflictException;
import io.github.lukasz756.marspay.paymentnode.idempotency.exceptions.IdempotencyRequestInProgressException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class IdempotencyExceptionHandler {

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    ProblemDetail handleKeyConflict(
            IdempotencyKeyConflictException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Idempotency key conflict");

        return problem;
    }

    @ExceptionHandler(IdempotencyRequestInProgressException.class)
    ProblemDetail handleRequestInProgress(
            IdempotencyRequestInProgressException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Idempotent request in progress");

        return problem;
    }
}