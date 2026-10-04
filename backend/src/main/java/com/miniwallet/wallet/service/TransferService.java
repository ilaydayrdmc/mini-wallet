package com.miniwallet.wallet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransferService(AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /** Gonderen hesaptaki TRANSFER_OUT kaydini dondurur. */
    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        Long fromId = request.fromAccountId();
        Long toId = request.toAccountId();
        if (fromId.equals(toId)) {
            throw new InvalidTransferException("Kendi hesabiniza transfer yapamazsiniz");
        }

        // Kilitleri her zaman kucuk id'den buyuge dogru al: A->B ile B->A ayni anda
        // gelirse birbirini beklemez (deadlock olmaz).
        Account from;
        Account to;
        if (fromId < toId) {
            from = lock(fromId);
            to = lock(toId);
        } else {
            to = lock(toId);
            from = lock(fromId);
        }

        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(fromId);
        }
        from.withdraw(request.amount());
        to.deposit(request.amount());

        Transaction out = transactionRepository.save(new Transaction(
                from, TransactionType.TRANSFER_OUT, request.amount(), from.getBalance()));
        transactionRepository.save(new Transaction(
                to, TransactionType.TRANSFER_IN, request.amount(), to.getBalance()));
        return TransactionResponse.from(out);
    }

    private Account lock(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }
}
