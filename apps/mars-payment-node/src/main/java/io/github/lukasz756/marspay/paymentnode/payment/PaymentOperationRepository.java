package io.github.lukasz756.marspay.paymentnode.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface PaymentOperationRepository
        extends JpaRepository<PaymentOperation, UUID> {

    List<PaymentOperation> findAllByPaymentIdOrderByCreatedAtAsc(
            UUID paymentId
    );
}