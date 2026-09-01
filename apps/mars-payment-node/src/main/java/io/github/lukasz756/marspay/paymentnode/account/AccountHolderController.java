package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/account-holders")
public class AccountHolderController {

    private final AccountService accountService;

    AccountHolderController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountHolderResponse> createAccountHolder(@Valid @RequestBody CreateAccountHolderRequest request) {

        AccountHolder accountHolder = accountService.createAccountHolder(
                request.reference(),
                request.type()
        );

        AccountHolderResponse response = AccountHolderResponse.from(accountHolder);

        URI location = URI.create(
                "/api/account-holders/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public AccountHolderResponse getAccountHolder(@PathVariable UUID id) {
        AccountHolder accountHolder = accountService.getAccountHolder(id);

        return AccountHolderResponse.from(accountHolder);
    }
}
