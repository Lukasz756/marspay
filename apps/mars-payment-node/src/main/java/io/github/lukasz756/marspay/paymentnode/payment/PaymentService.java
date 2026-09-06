package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.account.BalanceAccountRepository;
import io.github.lukasz756.marspay.paymentnode.account.exceptions.BalanceAccountNotFoundException;
import io.github.lukasz756.marspay.paymentnode.ledger.LedgerBalanceBucket;
import io.github.lukasz756.marspay.paymentnode.ledger.LedgerService;
import io.github.lukasz756.marspay.paymentnode.ledger.LedgerTransactionType;
import io.github.lukasz756.marspay.paymentnode.outbox.OutboxEventType;
import io.github.lukasz756.marspay.paymentnode.outbox.OutboxService;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.PaymentAlreadyExistsException;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.PaymentCurrencyMismatchException;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.PaymentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final PaymentOperationRepository paymentOperationRepository;
    private final LedgerService ledgerService;
    private final OutboxService outboxService;

    PaymentService(PaymentRepository paymentRepository,
                          BalanceAccountRepository balanceAccountRepository,
                          PaymentOperationRepository paymentOperationRepository,
                          LedgerService ledgerService,
                          OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.balanceAccountRepository = balanceAccountRepository;
        this.paymentOperationRepository = paymentOperationRepository;
        this.ledgerService = ledgerService;
        this.outboxService = outboxService;
    }

    @Transactional
    public Payment createPayment(
            UUID sourceBalanceAccountId,
            UUID targetBalanceAccountId,
            long amountMinor,
            String reference
    ) {
        BalanceAccount sourceBalanceAccount = balanceAccountRepository.findById(sourceBalanceAccountId).orElseThrow(() -> new BalanceAccountNotFoundException(sourceBalanceAccountId));
        BalanceAccount targetBalanceAccount = balanceAccountRepository.findById(targetBalanceAccountId).orElseThrow(() -> new BalanceAccountNotFoundException(targetBalanceAccountId));
        String sourceCurrency = sourceBalanceAccount.getCurrency();
        String targetCurrency = targetBalanceAccount.getCurrency();


        if (!Objects.equals(sourceCurrency, targetCurrency)) {
            throw new PaymentCurrencyMismatchException(sourceBalanceAccountId, sourceCurrency, targetBalanceAccountId, targetCurrency);
        }

        Payment payment = Payment.create(
                sourceBalanceAccountId,
                targetBalanceAccountId,
                amountMinor,
                sourceCurrency,
                reference
        );

        if (paymentRepository.existsBySourceBalanceAccountIdAndReference(
                sourceBalanceAccountId,
                payment.getReference()
        )) {
            throw new PaymentAlreadyExistsException(
                    sourceBalanceAccountId,
                    payment.getReference()
            );
        }
        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        recordOperation(savedPayment, PaymentOperationType.CREATE);

        outboxService.recordPaymentEvent(
                savedPayment,
                OutboxEventType.PAYMENT_CREATED
        );

        return savedPayment;
    }

    @Transactional
    public Payment authorizePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        BalanceAccount sourceAccount = balanceAccountRepository
                .findById(payment.getSourceBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getSourceBalanceAccountId()
                ));

        payment.authorize();
        sourceAccount.reserve(payment.getAmountMinor());

        recordOperation(payment, PaymentOperationType.AUTHORIZE);

        ledgerService.recordPaymentMovement(
                payment.getId(),
                LedgerTransactionType.PAYMENT_AUTHORIZE,
                payment.getCurrency(),
                payment.getReference(),
                sourceAccount.getId(),
                LedgerBalanceBucket.AVAILABLE,
                sourceAccount.getId(),
                LedgerBalanceBucket.RESERVED,
                payment.getAmountMinor()
        );

        outboxService.recordPaymentEvent(
                payment,
                OutboxEventType.PAYMENT_AUTHORIZED
        );

        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Transactional
    public Payment capturePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        BalanceAccount sourceAccount = balanceAccountRepository
                .findById(payment.getSourceBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getSourceBalanceAccountId()
                ));

        BalanceAccount targetAccount = balanceAccountRepository
                .findById(payment.getTargetBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getTargetBalanceAccountId()
                ));

        payment.capture();
        sourceAccount.captureReserved(payment.getAmountMinor());
        targetAccount.credit(payment.getAmountMinor());

        recordOperation(payment, PaymentOperationType.CAPTURE);

        ledgerService.recordPaymentMovement(
                payment.getId(),
                LedgerTransactionType.PAYMENT_CAPTURE,
                payment.getCurrency(),
                payment.getReference(),
                sourceAccount.getId(),
                LedgerBalanceBucket.RESERVED,
                targetAccount.getId(),
                LedgerBalanceBucket.AVAILABLE,
                payment.getAmountMinor()
        );

        outboxService.recordPaymentEvent(
                payment,
                OutboxEventType.PAYMENT_CAPTURED
        );

        return payment;
    }

    @Transactional
    public Payment cancelPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        boolean wasAuthorized =
                payment.getStatus() == PaymentStatus.AUTHORIZED;

        payment.cancel();

        if (wasAuthorized) {
            BalanceAccount sourceAccount = balanceAccountRepository
                    .findById(payment.getSourceBalanceAccountId())
                    .orElseThrow(() -> new BalanceAccountNotFoundException(
                            payment.getSourceBalanceAccountId()
                    ));

            sourceAccount.releaseReserved(payment.getAmountMinor());

            ledgerService.recordPaymentMovement(
                    payment.getId(),
                    LedgerTransactionType.PAYMENT_CANCEL,
                    payment.getCurrency(),
                    payment.getReference(),
                    sourceAccount.getId(),
                    LedgerBalanceBucket.RESERVED,
                    sourceAccount.getId(),
                    LedgerBalanceBucket.AVAILABLE,
                    payment.getAmountMinor()
            );
        }

        recordOperation(payment, PaymentOperationType.CANCEL);

        outboxService.recordPaymentEvent(
                payment,
                OutboxEventType.PAYMENT_CANCELLED
        );

        return payment;
    }

    @Transactional
    public Payment refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        BalanceAccount sourceAccount = balanceAccountRepository
                .findById(payment.getSourceBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getSourceBalanceAccountId()
                ));

        BalanceAccount targetAccount = balanceAccountRepository
                .findById(payment.getTargetBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getTargetBalanceAccountId()
                ));

        payment.refund();
        targetAccount.debit(payment.getAmountMinor());
        sourceAccount.credit(payment.getAmountMinor());

        recordOperation(payment, PaymentOperationType.REFUND);

        ledgerService.recordPaymentMovement(
                payment.getId(),
                LedgerTransactionType.PAYMENT_REFUND,
                payment.getCurrency(),
                payment.getReference(),
                targetAccount.getId(),
                LedgerBalanceBucket.AVAILABLE,
                sourceAccount.getId(),
                LedgerBalanceBucket.AVAILABLE,
                payment.getAmountMinor()
        );

        outboxService.recordPaymentEvent(
                payment,
                OutboxEventType.PAYMENT_REFUNDED
        );
        return payment;
    }

    @Transactional(readOnly = true)
    public List<PaymentOperation> getPaymentOperations(UUID paymentId) {
        if (!paymentRepository.existsById(paymentId)) {
            throw new PaymentNotFoundException(paymentId);
        }

        return paymentOperationRepository
                .findAllByPaymentIdOrderByCreatedAtAsc(paymentId);
    }


    private void recordOperation(
            Payment payment,
            PaymentOperationType type
    ) {
        PaymentOperation operation = PaymentOperation.record(
                payment.getId(),
                type,
                payment.getAmountMinor()
        );

        paymentOperationRepository.save(operation);
    }
}
