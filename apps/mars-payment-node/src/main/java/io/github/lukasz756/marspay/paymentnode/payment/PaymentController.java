package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.idempotency.IdempotencyResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotentPaymentCreationService paymentCreationService;

    public PaymentController(PaymentService paymentService, IdempotentPaymentCreationService paymentCreationService) {
        this.paymentService = paymentService;
        this.paymentCreationService = paymentCreationService;
    }

    @PostMapping
    public ResponseEntity<String> createPayment(
            @RequestHeader("Idempotency-Key")
            @NotBlank
            @Size(max = 255)
            String idempotencyKey,

            @Valid @RequestBody CreatePaymentRequest request
    ) {
        IdempotencyResult result =
                paymentCreationService.createPayment(
                        idempotencyKey,
                        request
                );

        ResponseEntity.BodyBuilder responseBuilder =
                ResponseEntity
                        .status(result.responseStatus())
                        .contentType(MediaType.APPLICATION_JSON);

        if (result.responseLocation() != null) {
            responseBuilder.location(
                    URI.create(result.responseLocation())
            );
        }

        if (result.replayed()) {
            responseBuilder.header(
                    "Idempotent-Replayed",
                    "true"
            );
        }

        return responseBuilder.body(result.responseBody());
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId
    ) {
        Payment payment = paymentService.getPayment(paymentId);

        return ResponseEntity.ok(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/authorize")
    public ResponseEntity<PaymentResponse> requestAuthorization(
            @PathVariable UUID paymentId
    ) {
        Payment payment =
                paymentService.requestAuthorization(paymentId);

        return ResponseEntity
                .accepted()
                .body(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/capture")
    public ResponseEntity<PaymentResponse> requestCapture(
            @PathVariable UUID paymentId
    ) {
        Payment payment =
                paymentService.requestCapture(paymentId);

        return ResponseEntity
                .accepted()
                .body(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> requestCancel(
            @PathVariable UUID paymentId
    ) {
        Payment payment =
                paymentService.requestCancel(paymentId);

        return ResponseEntity
                .accepted()
                .body(PaymentResponse.from(payment));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> requestRefund(
            @PathVariable UUID paymentId
    ) {
        Payment payment =
                paymentService.requestRefund(paymentId);

        return ResponseEntity
                .accepted()
                .body(PaymentResponse.from(payment));
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