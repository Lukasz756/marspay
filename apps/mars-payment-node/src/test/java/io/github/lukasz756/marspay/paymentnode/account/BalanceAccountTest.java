package io.github.lukasz756.marspay.paymentnode.account;

import io.github.lukasz756.marspay.paymentnode.account.exceptions.InsufficientBalanceException;
import io.github.lukasz756.marspay.paymentnode.account.exceptions.InsufficientReservedBalanceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceAccountTest {

    @Test
    void movesAvailableFundsToReserved() {
        BalanceAccount account = BalanceAccount.open(UUID.randomUUID(), "MCR");
        account.credit(250);
        account.reserve(100);

        assertThat(account.getReservedBalanceMinor()).isEqualTo(100);
        assertThat(account.getAvailableBalanceMinor()).isEqualTo(150);
    }

    @Test
    void doesNotChangeBalancesWhenReservationExceedsAvailableFunds() {
        BalanceAccount account = BalanceAccount.open(UUID.randomUUID(), "MCR");

        account.credit(200);
        assertThatThrownBy(() -> account.reserve(2000)).isInstanceOf(InsufficientBalanceException.class);
        assertThat(account.getAvailableBalanceMinor()).isEqualTo(200);
        assertThat(account.getReservedBalanceMinor()).isEqualTo(0);
    }

    @Test
    void capturesReservedFunds() {
        BalanceAccount account = BalanceAccount.open(UUID.randomUUID(), "MCR");
        account.credit(250);
        account.reserve(100);
        account.captureReserved(100);

        assertThat(account.getReservedBalanceMinor()).isEqualTo(0);
        assertThat(account.getAvailableBalanceMinor()).isEqualTo(150);
    }

    @Test
    void doesNotCaptureReservedFundsWhenExceedsReservedFunds() {
        BalanceAccount account = BalanceAccount.open(UUID.randomUUID(), "MCR");
        account.credit(250);
        account.reserve(100);

        assertThatThrownBy(() -> account.captureReserved(200)).isInstanceOf(InsufficientReservedBalanceException.class);
        assertThat(account.getAvailableBalanceMinor()).isEqualTo(150);
        assertThat(account.getReservedBalanceMinor()).isEqualTo(100);
    }

    @Test
    void releasesReservedFundsBackToAvailableBalance() {
        BalanceAccount account = BalanceAccount.open(UUID.randomUUID(), "MCR");
        account.credit(250);
        account.reserve(100);
        account.releaseReserved(100);

        assertThat(account.getAvailableBalanceMinor()).isEqualTo(250);
        assertThat(account.getReservedBalanceMinor()).isEqualTo(0);


    }

    @ParameterizedTest
    @ValueSource(longs = {0, -100})
    void rejectsNonPositiveReservation(long amountMinor) {
        BalanceAccount account = BalanceAccount.open(UUID.randomUUID(), "MCR");

        account.credit(250);

        assertThatThrownBy(() -> account.reserve(amountMinor)).isInstanceOf(IllegalArgumentException.class);

        assertThat(account.getAvailableBalanceMinor()).isEqualTo(250);
        assertThat(account.getReservedBalanceMinor()).isZero();
    }

    @Test
    void rejectsUnsupportedCurrency() {
        UUID accountHolderId = UUID.randomUUID();

        assertThatThrownBy(() -> BalanceAccount.open(accountHolderId, "EUR")).isInstanceOf(
                        IllegalArgumentException.class)
                .hasMessage("Currency must be MCR");
    }

    @Test
    void normalizesSupportedCurrency() {
        BalanceAccount account = BalanceAccount.open(UUID.randomUUID(), "mcr");

        assertThat(account.getCurrency()).isEqualTo("MCR");
    }
}