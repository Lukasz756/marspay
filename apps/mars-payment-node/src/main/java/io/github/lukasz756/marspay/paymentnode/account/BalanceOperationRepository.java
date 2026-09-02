package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface BalanceOperationRepository extends JpaRepository<BalanceOperation, UUID> {
    boolean existsByBalanceAccountIdAndReference(
            UUID balanceAccountId,
            String reference
    );
}
