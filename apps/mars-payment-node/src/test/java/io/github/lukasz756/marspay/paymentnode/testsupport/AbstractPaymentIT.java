package io.github.lukasz756.marspay.paymentnode.testsupport;

import io.github.lukasz756.marspay.paymentnode.account.AccountHolder;
import io.github.lukasz756.marspay.paymentnode.account.AccountHolderType;
import io.github.lukasz756.marspay.paymentnode.account.AccountService;
import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import io.github.lukasz756.marspay.paymentnode.inbox.InboxProcessor;
import io.github.lukasz756.marspay.paymentnode.outbox.OutboxTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
public abstract class AbstractPaymentIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected AccountService accountService;

    @MockitoBean
    protected OutboxTransport outboxTransport;

    @MockitoBean
    protected InboxProcessor inboxProcessor;

    protected PaymentFixture createPaymentFixture() {
        String suffix = UUID.randomUUID()
                .toString();

        AccountHolder sourceHolder = accountService.createAccountHolder("it-source-" + suffix,
                                                                        AccountHolderType.PERSON);

        AccountHolder targetHolder = accountService.createAccountHolder("it-target-" + suffix,
                                                                        AccountHolderType.PERSON);

        BalanceAccount sourceAccount = accountService.openBalanceAccount(sourceHolder.getId(), "MCR");

        BalanceAccount targetAccount = accountService.openBalanceAccount(targetHolder.getId(), "MCR");

        return new PaymentFixture(sourceAccount, targetAccount);
    }

    protected record PaymentFixture(BalanceAccount sourceAccount, BalanceAccount targetAccount) {
    }
}