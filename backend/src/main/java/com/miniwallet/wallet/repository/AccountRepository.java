package com.miniwallet.wallet.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.miniwallet.wallet.entity.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserIdOrderByIdAsc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    // SELECT ... FOR UPDATE: ayni hesaba es zamanli islemler sirayla yapilir.
    // Sahiplik sorgunun icinde: baskasinin hesabi "yok" gibi gorunur (404).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id and a.userId = :userId")
    Optional<Account> findByIdAndUserIdForUpdate(Long id, Long userId);

    // Transferde alici hesap icin: sahibi kim olursa olsun kilitlenir
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(Long id);
}
