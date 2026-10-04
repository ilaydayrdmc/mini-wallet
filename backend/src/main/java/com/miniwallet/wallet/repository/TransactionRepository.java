package com.miniwallet.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniwallet.wallet.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
