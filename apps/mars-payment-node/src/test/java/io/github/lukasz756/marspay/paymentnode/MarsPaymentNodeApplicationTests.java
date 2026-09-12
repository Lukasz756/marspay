package io.github.lukasz756.marspay.paymentnode;

import io.github.lukasz756.marspay.paymentnode.account.AccountHolder;
import io.github.lukasz756.marspay.paymentnode.account.AccountHolderType;
import io.github.lukasz756.marspay.paymentnode.account.AccountService;
import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.payment.Payment;
import io.github.lukasz756.marspay.paymentnode.payment.PaymentService;
import io.github.lukasz756.marspay.paymentnode.payment.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class MarsPaymentNodeApplicationTests {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PaymentService paymentService;

    @Test
    void contextLoads() {
    }

    @Test
    void connectsToPostgreSQL() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        assertThat(result).isOne();
    }

}