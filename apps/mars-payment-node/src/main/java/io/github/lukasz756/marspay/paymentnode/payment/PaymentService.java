package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.account.BalanceAccountRepository;
import io.github.lukasz756.marspay.paymentnode.account.exceptions.BalanceAccountNotFoundException;
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

    public PaymentService(PaymentRepository paymentRepository, BalanceAccountRepository balanceAccountRepository, PaymentOperationRepository paymentOperationRepository) {
        this.paymentRepository = paymentRepository;
        this.balanceAccountRepository = balanceAccountRepository;
        this.paymentOperationRepository = paymentOperationRepository;
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
        Payment savedPayment = paymentRepository.save(payment);

        recordOperation(savedPayment, PaymentOperationType.CREATE);

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
        }

        recordOperation(payment, PaymentOperationType.CANCEL);

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
