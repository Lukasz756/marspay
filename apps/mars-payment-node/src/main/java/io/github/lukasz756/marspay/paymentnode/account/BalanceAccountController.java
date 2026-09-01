package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
public class BalanceAccountController {

    private final AccountService accountService;


    public BalanceAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/api/account-holders/{accountHolderId}/balance-accounts")
    public ResponseEntity<BalanceAccountResponse> openBalanceAccount(
            @PathVariable UUID accountHolderId,
            @Valid @RequestBody OpenBalanceAccountRequest request
    ) {
        BalanceAccount balanceAccount = accountService.openBalanceAccount(accountHolderId, request.currency());

        BalanceAccountResponse response = BalanceAccountResponse.from(balanceAccount);

        URI location = URI.create(
                "/api/balance-accounts/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/balance-accounts/{balanceAccountId}")
    public ResponseEntity<BalanceAccountResponse> getBalanceAccount(
            @PathVariable UUID balanceAccountId
    ) {
        BalanceAccount balanceAccount =
                accountService.getBalanceAccount(balanceAccountId);

        return ResponseEntity.ok(
                BalanceAccountResponse.from(balanceAccount)
        );
    }
}
