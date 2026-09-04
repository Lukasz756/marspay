package io.github.lukasz756.marspay.paymentnode.payment;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        Payment payment = paymentService.createPayment(
                request.sourceBalanceAccountId(),
                request.targetBalanceAccountId(),
                request.amountMinor(),
                request.reference()
        );

        PaymentResponse response = PaymentResponse.from(payment);
        URI location = URI.create("/api/payments/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.getPayment(paymentId);

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<PaymentResponse> authorizePayment(
            @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.authorizePayment(paymentId);

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> capturePayment(
            @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.capturePayment(paymentId);

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.cancelPayment(paymentId);

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> refundPayment(
            @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.refundPayment(paymentId);

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @GetMapping("/{paymentId}/operations")
    public ResponseEntity<List<PaymentOperationResponse>> getPaymentOperations(
            @PathVariable UUID paymentId
    ) {
        List<PaymentOperationResponse> response = paymentService
                .getPaymentOperations(paymentId)
                .stream()
                .map(PaymentOperationResponse::from)
                .toList();

        return ResponseEntity.ok(response);
    }
}