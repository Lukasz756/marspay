package io.github.lukasz756.marspay.paymentnode.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BalanceTransferRepository extends JpaRepository<BalanceTransfer, UUID> {

    boolean existsBySourceBalanceAccountIdAndReference(UUID sourceBalanceAccountId, String reference);
}
