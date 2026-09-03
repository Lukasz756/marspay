package io.github.lukasz756.marspay.paymentnode.payment;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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
}