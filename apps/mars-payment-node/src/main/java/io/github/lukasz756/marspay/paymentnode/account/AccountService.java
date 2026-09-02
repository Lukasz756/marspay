package io.github.lukasz756.marspay.paymentnode.account;

import io.github.lukasz756.marspay.paymentnode.account.exceptions.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;


@Service
public class AccountService {

    private final AccountHolderRepository accountHolderRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final BalanceOperationRepository balanceOperationRepository;

    AccountService(AccountHolderRepository accountHolderRepository, BalanceAccountRepository balanceAccountRepository, BalanceOperationRepository balanceOperationRepository) {
        this.accountHolderRepository = accountHolderRepository;
        this.balanceAccountRepository = balanceAccountRepository;
        this.balanceOperationRepository = balanceOperationRepository;
    }

    @Transactional
    public AccountHolder createAccountHolder(
            String reference,
            AccountHolderType type
    ) {
        AccountHolder accountHolder = AccountHolder.create(reference, type);
        if (accountHolderRepository.existsByReference(accountHolder.getReference())) {
            throw new AccountHolderReferenceAlreadyExistsException(accountHolder.getReference());
        }
        return accountHolderRepository.save(accountHolder);
    }

    @Transactional(readOnly = true)
    public AccountHolder getAccountHolder(UUID accountHolderId) {
        return requireAccountHolder(accountHolderId);
    }

    @Transactional
    public BalanceAccount openBalanceAccount(UUID accountHolderId, String currency) {
        AccountHolder accountHolder = requireAccountHolder(accountHolderId);

        if (accountHolder.getStatus() != AccountHolderStatus.ACTIVE) {
            throw new AccountHolderNotActiveException(accountHolderId);
        }

        BalanceAccount balanceAccount = BalanceAccount.open(
                accountHolder.getId(),
                currency
        );

        if (balanceAccountRepository.existsByAccountHolderIdAndCurrency(
                accountHolder.getId(),
                balanceAccount.getCurrency()
        )) {
            throw new BalanceAccountAlreadyExistsException(
                    accountHolder.getId(),
                    balanceAccount.getCurrency()
            );
        }

        return balanceAccountRepository.save(balanceAccount);
    }

    @Transactional(readOnly = true)
    public BalanceAccount getBalanceAccount(UUID balanceAccountId) {
        return requireBalanceAccount(balanceAccountId);
    }

    @Transactional(readOnly = true)
    public List<BalanceAccount> getBalanceAccounts(UUID accountHolderId) {
        requireAccountHolder(accountHolderId);

        return balanceAccountRepository
                .findAllByAccountHolderIdOrderByCurrencyAsc(accountHolderId);
    }

    @Transactional
    public BalanceOperation creditBalanceAccount(
            UUID balanceAccountId,
            long amountMinor,
            String reference
    ) {
        BalanceAccount balanceAccount =
                requireBalanceAccount(balanceAccountId);

        balanceAccount.credit(amountMinor);

        BalanceOperation operation = BalanceOperation.credit(
                balanceAccount.getId(),
                amountMinor,
                reference,
                balanceAccount.getAvailableBalanceMinor(),
                balanceAccount.getReservedBalanceMinor()
        );

        if (balanceOperationRepository
                .existsByBalanceAccountIdAndReference(
                        balanceAccountId,
                        operation.getReference()
                )) {
            throw new BalanceOperationAlreadyExistsException(
                    balanceAccountId,
                    operation.getReference()
            );
        }

        return balanceOperationRepository.save(operation);
    }

    @Transactional(readOnly = true)
    public BalanceOperation getBalanceOperation(UUID operationId) {
        return balanceOperationRepository.findById(operationId)
                .orElseThrow(
                        () -> new BalanceOperationNotFoundException(operationId)
                );
    }

    private AccountHolder requireAccountHolder(UUID accountHolderId) {
        return accountHolderRepository.findById(accountHolderId)
                .orElseThrow(
                        () -> new AccountHolderNotFoundException(accountHolderId)
                );
    }

    private BalanceAccount requireBalanceAccount(UUID balanceAccountId) {
        return balanceAccountRepository.findById(balanceAccountId)
                .orElseThrow(
                        () -> new BalanceAccountNotFoundException(balanceAccountId)
                );
    }
}
