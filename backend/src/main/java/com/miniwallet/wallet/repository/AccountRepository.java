package com.miniwallet.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.miniwallet.wallet.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
