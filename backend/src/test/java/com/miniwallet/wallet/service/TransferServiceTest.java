package com.miniwallet.wallet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.dto.TransferRequest;
import com.miniwallet.wallet.entity.Account;
import com.miniwallet.wallet.entity.Transaction;
import com.miniwallet.wallet.entity.TransactionType;
import com.miniwallet.wallet.exception.AccountNotFoundException;
import com.miniwallet.wallet.exception.InsufficientFundsException;
import com.miniwallet.wallet.exception.InvalidTransferException;
import com.miniwallet.wallet.repository.AccountRepository;
import com.miniwallet.wallet.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferService transferService;

    private Account sender;
    private Account receiver;

    @BeforeEach
    void setUp() {
        sender = new Account("Ayse");
        sender.deposit(new BigDecimal("100.00"));
        receiver = new Account("Mehmet");
        receiver.deposit(new BigDecimal("20.00"));
    }

    private void stubAccounts(long senderId, long receiverId) {
        when(accountRepository.findByIdForUpdate(senderId)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(receiverId)).thenReturn(Optional.of(receiver));
    }

    @Test
    void transfer_movesMoneyAndRecordsBothSides() {
        stubAccounts(1L, 2L);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("30.00")));

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("50.00");
        assertThat(response.type()).isEqualTo(TransactionType.TRANSFER_OUT);
        assertThat(response.balanceAfter()).isEqualByComparingTo("70.00");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(captor.capture());
        List<Transaction> saved = captor.getAllValues();
        assertThat(saved).extracting(Transaction::getType)
                .containsExactlyInAnyOrder(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN);
        assertThat(saved).extracting(Transaction::getAmount)
                .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo("30.00"));
    }

    @Test
    void transfer_sameAccount_isRejectedWithoutTouchingDatabase() {
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 1L, BigDecimal.TEN)))
                .isInstanceOf(InvalidTransferException.class);

        verifyNoInteractions(accountRepository, transactionRepository);
    }

    @Test
    void transfer_insufficientBalance_throwsAndChangesNothing() {
        stubAccounts(1L, 2L);

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("100.01"))))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("20.00");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_unknownReceiver_throwsNotFound() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(accountRepository.findByIdForUpdate(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(1L, 77L, BigDecimal.ONE)))
                .isInstanceOf(AccountNotFoundException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void transfer_locksAccountsInAscendingIdOrder_whenSenderHasLowerId() {
        stubAccounts(1L, 2L);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transferService.transfer(new TransferRequest(1L, 2L, BigDecimal.ONE));

        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate(1L);
        order.verify(accountRepository).findByIdForUpdate(2L);
    }

    @Test
    void transfer_locksAccountsInAscendingIdOrder_whenSenderHasHigherId() {
        stubAccounts(2L, 1L);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transferService.transfer(new TransferRequest(2L, 1L, BigDecimal.ONE));

        // Gonderen 2 olsa bile once 1 numarali hesap kilitlenmeli (deadlock onleme)
        InOrder order = inOrder(accountRepository);
        order.verify(accountRepository).findByIdForUpdate(1L);
        order.verify(accountRepository).findByIdForUpdate(2L);
    }
}
