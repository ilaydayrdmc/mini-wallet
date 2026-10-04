package com.miniwallet.wallet.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.miniwallet.wallet.TestcontainersConfig;
import com.miniwallet.wallet.dto.AccountResponse;
import com.miniwallet.wallet.dto.AmountRequest;
import com.miniwallet.wallet.dto.CreateAccountRequest;
import com.miniwallet.wallet.dto.TransferRequest;
import com.miniwallet.wallet.entity.User;
import com.miniwallet.wallet.exception.InsufficientFundsException;
import com.miniwallet.wallet.repository.AccountRepository;
import com.miniwallet.wallet.repository.TransactionRepository;
import com.miniwallet.wallet.repository.UserRepository;
import com.miniwallet.wallet.service.AccountService;
import com.miniwallet.wallet.service.TransferService;

/**
 * Gercek PostgreSQL (Testcontainers) uzerinde es zamanlilik testleri.
 * Mock'lar kilitleme davranisini dogrulayamaz; bu testler FOR UPDATE kilidinin
 * ve kilit sirasinin gercekten calistigini gosterir.
 */
@Import(TestcontainersConfig.class)
@SpringBootTest
class ConcurrencyIntegrationTest {

    private static final BigDecimal ONE = BigDecimal.ONE;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    // Testteki tum hesaplar bu kullaniciya ait (hesaplarin gercek bir kullanici satiri olmasi gerekir)
    private Long userId;

    @BeforeEach
    void createUser() {
        userId = userRepository.save(new User("es.zamanli." + System.nanoTime(), "hash")).getId();
    }

    private AccountResponse newAccount(String name, String initialBalance) {
        AccountResponse account = accountService.create(userId, new CreateAccountRequest(name));
        if (new BigDecimal(initialBalance).signum() > 0) {
            accountService.deposit(userId, account.id(), new AmountRequest(new BigDecimal(initialBalance)));
        }
        return account;
    }

    private BigDecimal balanceOf(Long id) {
        return accountRepository.findById(id).orElseThrow().getBalance();
    }

    private long transactionCount(Long accountId) {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getAccount().getId().equals(accountId))
                .count();
    }

    /** Gorevleri ayni anda baslatir; 30 sn'de bitmezse (deadlock) test kirilir. */
    private <T> List<Future<T>> runConcurrently(List<Callable<T>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(pool.submit(() -> {
                start.await();
                return task.call();
            }));
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS))
                .as("islemler 30 sn icinde bitmedi (deadlock?)").isTrue();
        return futures;
    }

    @Test
    void concurrentDeposits_noUpdateIsLost() throws Exception {
        AccountResponse account = newAccount("Eszamanli yatirma", "0");

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> accountService.deposit(userId, account.id(), new AmountRequest(ONE)));
        }
        for (Future<Object> f : runConcurrently(tasks)) {
            f.get();
        }

        assertThat(balanceOf(account.id())).isEqualByComparingTo("20.00");
        assertThat(transactionCount(account.id())).isEqualTo(20);
    }

    @Test
    void concurrentWithdrawals_neverOverdraw() throws Exception {
        AccountResponse account = newAccount("Eszamanli cekme", "50");

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> {
                try {
                    accountService.withdraw(userId, account.id(), new AmountRequest(BigDecimal.TEN));
                    return true;
                } catch (InsufficientFundsException e) {
                    return false;
                }
            });
        }
        int succeeded = 0;
        for (Future<Boolean> f : runConcurrently(tasks)) {
            if (f.get()) {
                succeeded++;
            }
        }

        // 50 TL ile 10'ar TL'lik tam 5 cekme basarili olmali, bakiye eksiye dusmemeli
        assertThat(succeeded).isEqualTo(5);
        assertThat(balanceOf(account.id())).isEqualByComparingTo("0.00");
    }

    @Test
    void oppositeTransfers_conserveMoneyAndDoNotDeadlock() throws Exception {
        AccountResponse a = newAccount("Ayse", "1000");
        AccountResponse b = newAccount("Mehmet", "1000");

        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            tasks.add(() -> transferService.transfer(userId, new TransferRequest(a.id(), b.id(), BigDecimal.TEN)));
            tasks.add(() -> transferService.transfer(userId, new TransferRequest(b.id(), a.id(), BigDecimal.TEN)));
        }
        for (Future<Object> f : runConcurrently(tasks)) {
            f.get();
        }

        // Her yone 25 kez ayni tutar: bakiyeler degismemeli, toplam para sabit kalmali
        assertThat(balanceOf(a.id())).isEqualByComparingTo("1000.00");
        assertThat(balanceOf(b.id())).isEqualByComparingTo("1000.00");
        // 1 baslangic yatirmasi + 25 giden + 25 gelen
        assertThat(transactionCount(a.id())).isEqualTo(51);
        assertThat(transactionCount(b.id())).isEqualTo(51);
    }
}
