package io.github.lukasz756.marspay.paymentnode.payment;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/balance-transfers")
public class BalanceTransferController {

    private final BalanceTransferService balanceTransferService;

    BalanceTransferController(
            BalanceTransferService balanceTransferService
    ) {
        this.balanceTransferService = balanceTransferService;
    }

    @PostMapping
    public ResponseEntity<BalanceTransferResponse> createBalanceTransfer(
            @Valid @RequestBody CreateBalanceTransferRequest request
    ) {
        BalanceTransfer transfer = balanceTransferService.transfer(
                request.sourceBalanceAccountId(),
                request.targetBalanceAccountId(),
                request.amountMinor(),
                request.reference()
        );

        BalanceTransferResponse response =
                BalanceTransferResponse.from(transfer);

        URI location = URI.create(
                "/api/balance-transfers/" + response.id()
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<BalanceTransferResponse> getBalanceTransfer(
            @PathVariable UUID transferId
    ) {
        BalanceTransfer transfer =
                balanceTransferService.getBalanceTransfer(transferId);

        return ResponseEntity.ok(
                BalanceTransferResponse.from(transfer)
        );
    }
}