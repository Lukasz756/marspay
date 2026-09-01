package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler(
            AccountHolderReferenceAlreadyExistsException.class
    )
    ProblemDetail handleAccountHolderReferenceAlreadyExists(
            AccountHolderReferenceAlreadyExistsException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle(
                "Account holder reference already exists"
        );

        return problem;
    }

    @ExceptionHandler(AccountHolderNotFoundException.class)
    ProblemDetail handleAccountHolderNotFound(
            AccountHolderNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );

        problem.setTitle("Account holder not found");
        return problem;
    }
}
