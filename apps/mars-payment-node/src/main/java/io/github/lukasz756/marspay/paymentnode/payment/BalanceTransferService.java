package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.account.BalanceAccountRepository;
import io.github.lukasz756.marspay.paymentnode.account.BalanceOperation;
import io.github.lukasz756.marspay.paymentnode.account.BalanceOperationRepository;
import io.github.lukasz756.marspay.paymentnode.account.exceptions.BalanceAccountNotFoundException;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.BalanceTransferAlreadyExistsException;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.BalanceTransferCurrencyMismatchException;
import io.github.lukasz756.marspay.paymentnode.payment.exceptions.BalanceTransferNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BalanceTransferService {

    private final BalanceAccountRepository balanceAccountRepository;
    private final BalanceOperationRepository balanceOperationRepository;
    private final BalanceTransferRepository balanceTransferRepository;

    public BalanceTransferService(BalanceAccountRepository balanceAccountRepository,
                                  BalanceOperationRepository balanceOperationRepository,
                                  BalanceTransferRepository balanceTransferRepository) {
        this.balanceAccountRepository = balanceAccountRepository;
        this.balanceOperationRepository = balanceOperationRepository;
        this.balanceTransferRepository = balanceTransferRepository;
    }

    @Transactional
    public BalanceTransfer transfer(UUID sourceBalanceAccountId, UUID targetBalanceAccountId, long amountMinor,
                                    String reference) {
        BalanceAccount sourceAccount = requireBalanceAccount(sourceBalanceAccountId);

        BalanceAccount targetAccount = requireBalanceAccount(targetBalanceAccountId);

        requireSameCurrency(sourceAccount, targetAccount);

        BalanceTransfer transfer = BalanceTransfer.create(sourceAccount.getId(), targetAccount.getId(), amountMinor,
                                                          sourceAccount.getCurrency(), reference);

        if (balanceTransferRepository.existsBySourceBalanceAccountIdAndReference(sourceAccount.getId(),
                                                                                 transfer.getReference())) {
            throw new BalanceTransferAlreadyExistsException(sourceAccount.getId(), transfer.getReference());
        }

        BalanceTransfer savedTransfer = balanceTransferRepository.save(transfer);

        sourceAccount.debit(amountMinor);
        targetAccount.credit(amountMinor);

        BalanceOperation debitOperation = BalanceOperation.transferDebit(savedTransfer.getId(), sourceAccount.getId()
                , amountMinor, savedTransfer.getReference(), sourceAccount.getAvailableBalanceMinor(),
                                                                         sourceAccount.getReservedBalanceMinor());

        BalanceOperation creditOperation = BalanceOperation.transferCredit(savedTransfer.getId(),
                                                                           targetAccount.getId(), amountMinor,
                                                                           savedTransfer.getReference(),
                                                                           targetAccount.getAvailableBalanceMinor(),
                                                                           targetAccount.getReservedBalanceMinor());

        balanceOperationRepository.saveAll(List.of(debitOperation, creditOperation));

        return savedTransfer;
    }

    @Transactional(readOnly = true)
    public BalanceTransfer getBalanceTransfer(UUID transferId) {
        return balanceTransferRepository.findById(transferId)
                .orElseThrow(() -> new BalanceTransferNotFoundException(transferId));
    }

    private BalanceAccount requireBalanceAccount(UUID balanceAccountId) {
        return balanceAccountRepository.findById(balanceAccountId)
                .orElseThrow(() -> new BalanceAccountNotFoundException(balanceAccountId));
    }

    private void requireSameCurrency(BalanceAccount sourceAccount, BalanceAccount targetAccount) {
        if (!sourceAccount.getCurrency()
                .equals(targetAccount.getCurrency())) {
            throw new BalanceTransferCurrencyMismatchException(sourceAccount.getId(), sourceAccount.getCurrency(),
                                                               targetAccount.getId(), targetAccount.getCurrency());
        }
    }
}
