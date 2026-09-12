package io.github.lukasz756.marspay.paymentnode.ledger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private final LedgerTransactionRepository transactionRepository;
    private final LedgerEntryRepository entryRepository;

    LedgerService(LedgerTransactionRepository transactionRepository, LedgerEntryRepository entryRepository) {
        this.transactionRepository = transactionRepository;
        this.entryRepository = entryRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordPaymentMovement(UUID paymentId, LedgerTransactionType type, String currency, String reference,
                                      UUID sourceBalanceAccountId, LedgerBalanceBucket sourceBucket,
                                      UUID targetBalanceAccountId, LedgerBalanceBucket targetBucket, long amountMinor) {
        LedgerTransaction transaction = LedgerTransaction.forPayment(paymentId, type, currency, reference);

        LedgerTransaction savedTransaction = transactionRepository.saveAndFlush(transaction);

        LedgerEntry decreaseEntry = LedgerEntry.decrease(savedTransaction.getId(), sourceBalanceAccountId,
                                                         sourceBucket, amountMinor);

        LedgerEntry increaseEntry = LedgerEntry.increase(savedTransaction.getId(), targetBalanceAccountId,
                                                         targetBucket, amountMinor);

        List<LedgerEntry> entries = List.of(decreaseEntry, increaseEntry);

        validateBalanced(entries);

        entryRepository.saveAll(entries);
    }

    @Transactional(readOnly = true)
    public List<LedgerTransactionResponse> getPaymentLedger(UUID paymentId) {
        return transactionRepository.findAllByPaymentIdOrderByCreatedAtAsc(paymentId)
                .stream()
                .map(transaction -> {
                    List<LedgerEntry> entries =
                            entryRepository.findAllByLedgerTransactionIdOrderByCreatedAtAsc(transaction.getId());

                    return LedgerTransactionResponse.from(transaction, entries);
                })
                .toList();
    }

    private void validateBalanced(List<LedgerEntry> entries) {
        if (entries.size() < 2) {
            throw new IllegalArgumentException("Ledger transaction must contain at least two entries");
        }

        long total = 0;

        try {
            for (LedgerEntry entry : entries) {
                total = Math.addExact(total, entry.getAmountMinor());
            }
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Ledger transaction total exceeds supported range", exception);
        }

        if (total != 0) {
            throw new IllegalArgumentException("Ledger transaction entries must sum to 0");
        }
    }
}