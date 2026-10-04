package com.miniwallet.wallet.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniwallet.wallet.dto.AccountResponse;
import com.miniwallet.wallet.dto.CreateAccountRequest;
import com.miniwallet.wallet.dto.AmountRequest;
import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.entity.Account;
import com.miniwallet.wallet.entity.Transaction;
import com.miniwallet.wallet.entity.TransactionType;
import com.miniwallet.wallet.exception.AccountNotFoundException;
import com.miniwallet.wallet.exception.InsufficientFundsException;
import com.miniwallet.wallet.repository.AccountRepository;
import com.miniwallet.wallet.repository.TransactionRepository;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        Account account = accountRepository.save(new Account(request.ownerName().trim()));
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public TransactionResponse deposit(Long accountId, AmountRequest request) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.deposit(request.amount());
        Transaction tx = transactionRepository.save(new Transaction(
                account, TransactionType.DEPOSIT, request.amount(), account.getBalance()));
        return TransactionResponse.from(tx);
    }

    @Transactional(readOnly = true)
    public PagedModel<TransactionResponse> getTransactions(Long accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        // En yeni islem en uste; ayni ana denk gelenlerde id ile sirala
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return new PagedModel<>(
                transactionRepository.findByAccountId(accountId, pageable)
                        .map(TransactionResponse::from));
    }

    @Transactional
    public TransactionResponse withdraw(Long accountId, AmountRequest request) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException(accountId);
        }
        account.withdraw(request.amount());
        Transaction tx = transactionRepository.save(new Transaction(
                account, TransactionType.WITHDRAWAL, request.amount(), account.getBalance()));
        return TransactionResponse.from(tx);
    }
}
