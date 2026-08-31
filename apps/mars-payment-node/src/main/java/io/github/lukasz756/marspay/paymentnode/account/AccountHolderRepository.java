package io.github.lukasz756.marspay.paymentnode.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AccountHolderRepository extends JpaRepository<AccountHolder, UUID> {
}
