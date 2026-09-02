package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/balance-operations")
public class BalanceOperationController {

    private final AccountService accountService;

    public BalanceOperationController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{operationId}")
    public ResponseEntity<BalanceOperationResponse> getBalanceOperation(
            @PathVariable UUID operationId
    ) {
        BalanceOperation operation =
                accountService.getBalanceOperation(operationId);

        return ResponseEntity.ok(
                BalanceOperationResponse.from(operation)
        );
    }
}