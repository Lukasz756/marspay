package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/account-holders/{accountHolderId}/balance-accounts")
public class BalanceAccountController {

    private final AccountService accountService;


    public BalanceAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<OpenBalanceAccountResponse> openBalanceAccount(
            @PathVariable UUID accountHolderId,
            @Valid @RequestBody OpenBalanceAccountRequest request
    ) {
        BalanceAccount balanceAccount = accountService.openBalanceAccount(accountHolderId, request.currency());

        OpenBalanceAccountResponse response = OpenBalanceAccountResponse.from(balanceAccount);

        URI location = URI.create(
                "/api/balance-accounts/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }
}
