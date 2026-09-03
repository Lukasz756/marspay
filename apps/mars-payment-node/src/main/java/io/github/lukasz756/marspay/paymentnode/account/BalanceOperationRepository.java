package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BalanceOperationRepository extends JpaRepository<BalanceOperation, UUID> {
    boolean existsByBalanceAccountIdAndReference(
            UUID balanceAccountId,
            String reference
    );
    List<BalanceOperation> findAllByBalanceAccountIdOrderByCreatedAtDesc(
            UUID balanceAccountId
    );
}
