package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BalanceAccountRepository extends JpaRepository<BalanceAccount, UUID> {
    boolean existsByAccountHolderIdAndCurrency(
            UUID accountHolderId,
            String currency
    );

    List<BalanceAccount> findAllByAccountHolderIdOrderByCurrencyAsc(
            UUID accountHolderId
    );
}
