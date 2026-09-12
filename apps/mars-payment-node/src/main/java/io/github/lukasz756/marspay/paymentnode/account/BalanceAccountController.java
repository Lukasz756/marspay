package io.github.lukasz756.marspay.paymentnode.account;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
public class BalanceAccountController {

    private final AccountService accountService;


    public BalanceAccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/api/account-holders/{accountHolderId}/balance-accounts")
    public ResponseEntity<BalanceAccountResponse> openBalanceAccount(@PathVariable UUID accountHolderId,
                                                                     @Valid @RequestBody OpenBalanceAccountRequest request) {
        BalanceAccount balanceAccount = accountService.openBalanceAccount(accountHolderId, request.currency());

        BalanceAccountResponse response = BalanceAccountResponse.from(balanceAccount);

        URI location = URI.create("/api/balance-accounts/" + response.id());

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/api/balance-accounts/{balanceAccountId}")
    public ResponseEntity<BalanceAccountResponse> getBalanceAccount(@PathVariable UUID balanceAccountId) {
        BalanceAccount balanceAccount = accountService.getBalanceAccount(balanceAccountId);

        return ResponseEntity.ok(BalanceAccountResponse.from(balanceAccount));
    }

    @GetMapping("/api/account-holders/{accountHolderId}/balance-accounts")
    public ResponseEntity<List<BalanceAccountResponse>> getBalanceAccounts(@PathVariable UUID accountHolderId) {
        List<BalanceAccountResponse> response =
                accountService.getBalanceAccounts(accountHolderId)
                        .stream()
                        .map(BalanceAccountResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/balance-accounts/{balanceAccountId}/credits")
    public ResponseEntity<BalanceOperationResponse> creditBalanceAccount(@PathVariable UUID balanceAccountId,
                                                                         @Valid @RequestBody CreditBalanceAccountRequest request) {
        BalanceOperation operation = accountService.creditBalanceAccount(balanceAccountId, request.amountMinor(),
                                                                         request.reference());

        BalanceOperationResponse response = BalanceOperationResponse.from(operation);

        URI location = URI.create("/api/balance-operations/" + response.id());

        return ResponseEntity.created(location)
                .body(response);
    }

    @GetMapping("/api/balance-accounts/{balanceAccountId}/operations")
    public ResponseEntity<List<BalanceOperationResponse>> getBalanceOperations(@PathVariable UUID balanceAccountId) {
        List<BalanceOperationResponse> response =
                accountService.getBalanceOperations(balanceAccountId)
                        .stream()
                        .map(BalanceOperationResponse::from)
                        .toList();

        return ResponseEntity.ok(response);
    }
}
