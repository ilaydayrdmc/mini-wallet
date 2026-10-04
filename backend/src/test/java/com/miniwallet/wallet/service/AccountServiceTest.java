package com.miniwallet.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.miniwallet.wallet.dto.AccountResponse;
import com.miniwallet.wallet.dto.AmountRequest;
import com.miniwallet.wallet.dto.CreateAccountRequest;
import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.entity.Account;
import com.miniwallet.wallet.entity.Transaction;
import com.miniwallet.wallet.entity.TransactionType;
import com.miniwallet.wallet.exception.AccountNotFoundException;
import com.miniwallet.wallet.exception.InsufficientFundsException;
import com.miniwallet.wallet.repository.AccountRepository;
import com.miniwallet.wallet.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountService accountService;

    private static final Long USER_ID = 7L;

    private Account account;

    @BeforeEach
    void setUp() {
        account = new Account(USER_ID, "Ayse");
        account.deposit(new BigDecimal("100.00"));
    }

    @Test
    void deposit_increasesBalanceAndRecordsTransaction() {
        when(accountRepository.findByIdAndUserIdForUpdate(1L, USER_ID)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response =
                accountService.deposit(USER_ID, 1L, new AmountRequest(new BigDecimal("25.50")));

        assertThat(account.getBalance()).isEqualByComparingTo("125.50");
        assertThat(response.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(response.amount()).isEqualByComparingTo("25.50");
        assertThat(response.balanceAfter()).isEqualByComparingTo("125.50");
    }

    @Test
    void deposit_unknownAccount_throwsNotFound() {
        when(accountRepository.findByIdAndUserIdForUpdate(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deposit(USER_ID, 99L, new AmountRequest(BigDecimal.TEN)))
                .isInstanceOf(AccountNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void withdraw_decreasesBalanceAndRecordsTransaction() {
        when(accountRepository.findByIdAndUserIdForUpdate(1L, USER_ID)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response =
                accountService.withdraw(USER_ID, 1L, new AmountRequest(new BigDecimal("40.00")));

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
        assertThat(response.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(response.balanceAfter()).isEqualByComparingTo("60.00");
    }

    @Test
    void withdraw_exactBalance_isAllowed() {
        when(accountRepository.findByIdAndUserIdForUpdate(1L, USER_ID)).thenReturn(Optional.of(account));
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.withdraw(USER_ID, 1L, new AmountRequest(new BigDecimal("100.00")));

        assertThat(account.getBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void withdraw_insufficientBalance_throwsAndKeepsBalance() {
        when(accountRepository.findByIdAndUserIdForUpdate(1L, USER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() ->
                accountService.withdraw(USER_ID, 1L, new AmountRequest(new BigDecimal("100.01"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void create_assignsAccountToRequestingUser() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountService.create(USER_ID, new CreateAccountRequest("  Ayse Yilmaz "));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getOwnerName()).isEqualTo("Ayse Yilmaz");
    }

    @Test
    void findAll_returnsOnlyRequestingUsersAccounts() {
        when(accountRepository.findByUserIdOrderByIdAsc(USER_ID)).thenReturn(List.of(account));

        List<AccountResponse> result = accountService.findAll(USER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).ownerName()).isEqualTo("Ayse");
        verify(accountRepository, never()).findAll();
    }

    @Test
    void getTransactions_ofSomeoneElsesAccount_throwsNotFound() {
        when(accountRepository.existsByIdAndUserId(1L, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> accountService.getTransactions(USER_ID, 1L, 0, 20))
                .isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(transactionRepository);
    }

    @Test
    void withdraw_unknownAccount_throwsNotFound() {
        when(accountRepository.findByIdAndUserIdForUpdate(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.withdraw(USER_ID, 99L, new AmountRequest(BigDecimal.ONE)))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
