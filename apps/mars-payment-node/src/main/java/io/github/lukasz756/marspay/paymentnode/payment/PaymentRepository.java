package io.github.lukasz756.marspay.paymentnode.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PaymentRepository extends JpaRepository<Payment, UUID> {

    boolean existsBySourceBalanceAccountIdAndReference(UUID sourceBalanceAccountId, String reference);
}