package io.github.lukasz756.marspay.paymentnode.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
class AccountPersistenceTest {

    public static final String HOLDER_001 = "holder-001";

    @Autowired
    private AccountHolderRepository accountHolderRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BalanceAccountRepository balanceAccountRepository;

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRESQL =
            new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void savesAndLoadsAccountHolder() {
        AccountHolder accountHolder = AccountHolder.create(HOLDER_001, AccountHolderType.PERSON);
        accountHolderRepository.saveAndFlush(accountHolder);
        UUID accountHolderId = accountHolder.getId();
        entityManager.clear();
        AccountHolder accHolderFromRepo = accountHolderRepository.findById(accountHolderId).orElseThrow();

        assertThat(accountHolderId).isNotNull();
        assertThat(accHolderFromRepo.getId()).isEqualTo(accountHolderId);
        assertThat(accHolderFromRepo.getReference()).isEqualTo(HOLDER_001);
        assertThat(accHolderFromRepo.getType()).isEqualTo(AccountHolderType.PERSON);
        assertThat(accHolderFromRepo.getStatus()).isEqualTo(AccountHolderStatus.ACTIVE);
        assertThat(accHolderFromRepo.getCreatedAt()).isNotNull();
        assertThat(accHolderFromRepo.getUpdatedAt()).isNotNull();
    }

    @Test
    void savesAndLoadsBalanceAccount() {
        AccountHolder accountHolder = AccountHolder.create(HOLDER_001, AccountHolderType.PERSON);
        accountHolderRepository.saveAndFlush(accountHolder);
        AccountHolder accHolderFromRepo = accountHolderRepository.findById(accountHolder.getId()).orElseThrow();
        BalanceAccount balanceAccount = BalanceAccount.open(accHolderFromRepo.getId(), "MCR");
        balanceAccountRepository.saveAndFlush(balanceAccount);
        entityManager.clear();
        UUID balanceAccountId = balanceAccount.getId();
        BalanceAccount balanceAccountFromRepo = balanceAccountRepository.findById(balanceAccountId).orElseThrow();

        assertThat(balanceAccountFromRepo.getId()).isNotNull();
        assertThat(balanceAccountFromRepo.getId()).isEqualTo(balanceAccountId);
        assertThat(balanceAccountFromRepo.getAccountHolderId()).isEqualTo(accHolderFromRepo.getId());
        assertThat(balanceAccountFromRepo.getCurrency()).isEqualTo("MCR");
        assertThat(balanceAccountFromRepo.getAvailableBalanceMinor()).isEqualTo(0);
        assertThat(balanceAccountFromRepo.getReservedBalanceMinor()).isEqualTo(0);
        assertThat(balanceAccountFromRepo.getStatus()).isEqualTo(BalanceAccountStatus.ACTIVE);
        assertThat(balanceAccountFromRepo.getCreatedAt()).isNotNull();
        assertThat(balanceAccountFromRepo.getUpdatedAt()).isNotNull();
        assertThat(balanceAccountFromRepo.getVersion()).isZero();
    }
}