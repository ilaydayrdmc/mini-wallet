package com.miniwallet.wallet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.miniwallet.wallet.dto.AccountResponse;
import com.miniwallet.wallet.dto.CreateAccountRequest;
import com.miniwallet.wallet.dto.DepositRequest;
import com.miniwallet.wallet.dto.TransactionResponse;
import com.miniwallet.wallet.entity.Account;
import com.miniwallet.wallet.entity.Transaction;
import com.miniwallet.wallet.entity.TransactionType;
import com.miniwallet.wallet.exception.AccountNotFoundException;
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
    public TransactionResponse deposit(Long accountId, DepositRequest request) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        account.deposit(request.amount());
        Transaction tx = transactionRepository.save(new Transaction(
                account, TransactionType.DEPOSIT, request.amount(), account.getBalance()));
        return TransactionResponse.from(tx);
    }
}
