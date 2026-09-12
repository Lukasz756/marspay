package io.github.lukasz756.marspay.paymentnode.payment;

import io.github.lukasz756.marspay.paymentnode.account.AccountHolder;
import io.github.lukasz756.marspay.paymentnode.account.AccountHolderType;
import io.github.lukasz756.marspay.paymentnode.account.AccountService;
import io.github.lukasz756.marspay.paymentnode.account.BalanceAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class PaymentRefundIT {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer POSTGRESQL = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private AccountService accountService;

    @Autowired
    private PaymentService paymentService;

    @Test
    void reservesAndUsesTargetFundsWhenRefundIsConfirmed() {
        RefundFixture fixture = createCapturedPayment("confirmed");

        Payment payment = fixture.payment();
        BalanceAccount sourceAccount = fixture.sourceAccount();
        BalanceAccount targetAccount = fixture.targetAccount();

        paymentService.requestRefund(payment.getId());

        BalanceAccount refreshedTarget = accountService.getBalanceAccount(targetAccount.getId());

        Payment refreshedPayment = paymentService.getPayment(payment.getId());

        assertThat(refreshedTarget.getAvailableBalanceMinor()).isZero();
        assertThat(refreshedTarget.getReservedBalanceMinor()).isEqualTo(300);
        assertThat(refreshedPayment.getStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);

        paymentService.confirmRefund(payment.getId());

        BalanceAccount refundedSource = accountService.getBalanceAccount(sourceAccount.getId());

        BalanceAccount refundedTarget = accountService.getBalanceAccount(targetAccount.getId());

        Payment refundedPayment = paymentService.getPayment(payment.getId());

        assertThat(refundedSource.getAvailableBalanceMinor()).isEqualTo(1_000);
        assertThat(refundedSource.getReservedBalanceMinor()).isZero();

        assertThat(refundedTarget.getAvailableBalanceMinor()).isZero();
        assertThat(refundedTarget.getReservedBalanceMinor()).isZero();

        assertThat(refundedPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void releasesTargetFundsWhenRefundFails() {
        RefundFixture fixture = createCapturedPayment("failed");

        Payment payment = fixture.payment();
        BalanceAccount sourceAccount = fixture.sourceAccount();
        BalanceAccount targetAccount = fixture.targetAccount();

        paymentService.requestRefund(payment.getId());
        paymentService.failRefund(payment.getId());

        BalanceAccount restoredSource = accountService.getBalanceAccount(sourceAccount.getId());

        BalanceAccount restoredTarget = accountService.getBalanceAccount(targetAccount.getId());

        Payment failedRefundPayment = paymentService.getPayment(payment.getId());

        assertThat(restoredSource.getAvailableBalanceMinor()).isEqualTo(700);
        assertThat(restoredSource.getReservedBalanceMinor()).isZero();

        assertThat(restoredTarget.getAvailableBalanceMinor()).isEqualTo(300);
        assertThat(restoredTarget.getReservedBalanceMinor()).isZero();

        assertThat(failedRefundPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    private RefundFixture createCapturedPayment(String scenario) {
        AccountHolder sourceHolder = accountService.createAccountHolder("refund-source-" + scenario,
                                                                        AccountHolderType.PERSON);

        AccountHolder targetHolder = accountService.createAccountHolder("refund-target-" + scenario,
                                                                        AccountHolderType.PERSON);

        BalanceAccount sourceAccount = accountService.openBalanceAccount(sourceHolder.getId(), "MCR");

        BalanceAccount targetAccount = accountService.openBalanceAccount(targetHolder.getId(), "MCR");

        accountService.creditBalanceAccount(sourceAccount.getId(), 1_000, "refund-funding-" + scenario);

        Payment payment = paymentService.createPayment(sourceAccount.getId(), targetAccount.getId(), 300, "refund" +
                "-payment-" + scenario);

        paymentService.requestAuthorization(payment.getId());
        paymentService.confirmAuthorization(payment.getId());
        paymentService.requestCapture(payment.getId());
        paymentService.confirmCapture(payment.getId());

        return new RefundFixture(payment, sourceAccount, targetAccount);
    }

    private record RefundFixture(Payment payment, BalanceAccount sourceAccount, BalanceAccount targetAccount) {
    }
}