package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


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

    @Transactional(readOnly = true)
    public AccountHolder getAccountHolder(UUID id) {
        return accountHolderRepository.findById(id)
                .orElseThrow(
                        () -> new AccountHolderNotFoundException(id)
                );
    }
}