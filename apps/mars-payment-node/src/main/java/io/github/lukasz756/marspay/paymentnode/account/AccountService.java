package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AccountService {

    private final AccountHolderRepository accountHolderRepository;

    AccountService(AccountHolderRepository accountHolderRepository) {
        this.accountHolderRepository = accountHolderRepository;
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
}