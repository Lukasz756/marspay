package io.github.lukasz756.marspay.paymentnode.ledger;

import io.github.lukasz756.marspay.paymentnode.payment.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class LedgerController {

    private final LedgerService ledgerService;
    private final PaymentService paymentService;

    public LedgerController(
            LedgerService ledgerService,
            PaymentService paymentService
    ) {
        this.ledgerService = ledgerService;
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}/ledger")
    public ResponseEntity<List<LedgerTransactionResponse>> getPaymentLedger(
            @PathVariable UUID paymentId
    ) {
        paymentService.getPayment(paymentId);

        return ResponseEntity.ok(
                ledgerService.getPaymentLedger(paymentId)
        );
    }
}