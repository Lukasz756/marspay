package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
public class AccountService {

    private final AccountHolderRepository accountHolderRepository;
    private final BalanceAccountRepository balanceAccountRepository;

    AccountService(AccountHolderRepository accountHolderRepository, BalanceAccountRepository balanceAccountRepository) {
        this.accountHolderRepository = accountHolderRepository;
        this.balanceAccountRepository = balanceAccountRepository;
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

        return balanceAccountRepository.save(balanceAccount);
    }

    private AccountHolder requireAccountHolder(UUID accountHolderId) {
        return accountHolderRepository.findById(accountHolderId)
                .orElseThrow(
                        () -> new AccountHolderNotFoundException(accountHolderId)
                );
    }
}