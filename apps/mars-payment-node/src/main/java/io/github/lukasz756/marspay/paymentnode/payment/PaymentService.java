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
    public Payment requestAuthorization(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        BalanceAccount sourceAccount = balanceAccountRepository
                .findById(payment.getSourceBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getSourceBalanceAccountId()
                ));

        payment.requestAuthorization();
        sourceAccount.reserve(payment.getAmountMinor());

        recordOperation(
                payment,
                PaymentOperationType.AUTHORIZATION_REQUESTED
        );

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
                OutboxEventType.PAYMENT_AUTHORIZATION_REQUESTED
        );

        return payment;
    }

    @Transactional
    public Payment confirmAuthorization(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.confirmAuthorization();

        recordOperation(
                payment,
                PaymentOperationType.AUTHORIZATION_CONFIRMED
        );

        return payment;
    }

    @Transactional
    public Payment declineAuthorization(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        BalanceAccount sourceAccount = balanceAccountRepository
                .findById(payment.getSourceBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getSourceBalanceAccountId()
                ));

        payment.declineAuthorization();
        sourceAccount.releaseReserved(payment.getAmountMinor());

        recordOperation(
                payment,
                PaymentOperationType.AUTHORIZATION_DECLINED
        );

        ledgerService.recordPaymentMovement(
                payment.getId(),
                LedgerTransactionType.PAYMENT_AUTHORIZATION_DECLINED,
                payment.getCurrency(),
                payment.getReference(),
                sourceAccount.getId(),
                LedgerBalanceBucket.RESERVED,
                sourceAccount.getId(),
                LedgerBalanceBucket.AVAILABLE,
                payment.getAmountMinor()
        );

        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Transactional
    public Payment requestCapture(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.requestCapture();

        recordOperation(
                payment,
                PaymentOperationType.CAPTURE_REQUESTED
        );

        outboxService.recordPaymentEvent(
                payment,
                OutboxEventType.PAYMENT_CAPTURE_REQUESTED
        );

        return payment;
    }

    @Transactional
    public Payment confirmCapture(UUID paymentId) {
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

        payment.confirmCapture();
        sourceAccount.captureReserved(payment.getAmountMinor());
        targetAccount.credit(payment.getAmountMinor());

        recordOperation(
                payment,
                PaymentOperationType.CAPTURE_CONFIRMED
        );

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

        return payment;
    }

    @Transactional
    public Payment failCapture(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.failCapture();

        recordOperation(
                payment,
                PaymentOperationType.CAPTURE_FAILED
        );

        return payment;
    }

    @Transactional
    public Payment requestCancel(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.requestCancel();

        recordOperation(
                payment,
                PaymentOperationType.CANCEL_REQUESTED
        );

        outboxService.recordPaymentEvent(
                payment,
                OutboxEventType.PAYMENT_CANCEL_REQUESTED
        );

        return payment;
    }

    @Transactional
    public Payment confirmCancel(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        BalanceAccount sourceAccount = balanceAccountRepository
                .findById(payment.getSourceBalanceAccountId())
                .orElseThrow(() -> new BalanceAccountNotFoundException(
                        payment.getSourceBalanceAccountId()
                ));

        payment.confirmCancel();
        sourceAccount.releaseReserved(payment.getAmountMinor());

        recordOperation(
                payment,
                PaymentOperationType.CANCEL_CONFIRMED
        );

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

        return payment;
    }

    @Transactional
    public Payment failCancel(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.failCancel();

        recordOperation(
                payment,
                PaymentOperationType.CANCEL_FAILED
        );

        return payment;
    }

    @Transactional
    public Payment requestRefund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.requestRefund();

        recordOperation(
                payment,
                PaymentOperationType.REFUND_REQUESTED
        );

        outboxService.recordPaymentEvent(
                payment,
                OutboxEventType.PAYMENT_REFUND_REQUESTED
        );

        return payment;
    }

    @Transactional
    public Payment confirmRefund(UUID paymentId) {
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

        payment.confirmRefund();
        targetAccount.debit(payment.getAmountMinor());
        sourceAccount.credit(payment.getAmountMinor());

        recordOperation(
                payment,
                PaymentOperationType.REFUND_CONFIRMED
        );

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

        return payment;
    }

    @Transactional
    public Payment failRefund(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.failRefund();

        recordOperation(
                payment,
                PaymentOperationType.REFUND_FAILED
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
