package com.miniwallet.wallet.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.miniwallet.wallet.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Metot adindan sorgu uretilir: select ... where account_id = ? (+ order by, limit)
    Page<Transaction> findByAccountId(Long accountId, Pageable pageable);
}
