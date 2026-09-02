package io.github.lukasz756.marspay.paymentnode.account;

import io.github.lukasz756.marspay.paymentnode.account.exceptions.*;
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

    @ExceptionHandler(AccountHolderNotActiveException.class)
    ProblemDetail handleAccountHolderNotActiveException(
            AccountHolderNotActiveException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
        problem.setTitle("Account holder not active");
        return problem;
    }

    @ExceptionHandler(BalanceAccountAlreadyExistsException.class)
    ProblemDetail handleBalanceAccountAlreadyExists(
            BalanceAccountAlreadyExistsException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Balance account already exists");
        return problem;
    }

    @ExceptionHandler(BalanceAccountNotFoundException.class)
    ProblemDetail handleBalanceAccountNotFoundException(
            BalanceAccountNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setTitle("Balance account not found");
        return problem;
    }

    @ExceptionHandler(BalanceAccountNotActiveException.class)
    ProblemDetail handleBalanceAccountNotActive(
            BalanceAccountNotActiveException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Balance account not active");
        return problem;
    }

    @ExceptionHandler(BalanceOperationAlreadyExistsException.class)
    ProblemDetail handleBalanceOperationAlreadyExists(
            BalanceOperationAlreadyExistsException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );

        problem.setTitle("Balance operation already exists");
        return problem;
    }

    @ExceptionHandler(BalanceOperationNotFoundException.class)
    ProblemDetail handleBalanceOperationNotFoundException(
            BalanceOperationNotFoundException exception
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setTitle("Balance operation not found");
        return problem;
    }
}
