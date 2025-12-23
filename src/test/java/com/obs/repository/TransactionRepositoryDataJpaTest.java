
package com.obs.repository;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryDataJpaTest {

    @Autowired TransactionRepository transactionRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired UserRepository userRepository;

    private User newUser(String username) {
        User u = new User();
        u.setUsername(username);
        u.setPassword("pw");
        u.setEmail(username + "@example.com");
        u.setPhoneNumber("9999999999");
        u.setFullName("Full " + username);
        return userRepository.save(u);
    }

    private Account newAccount(User user, String accNo) {
        Account a = new Account();
        a.setAccountNumber(accNo);
        a.setUser(user);
        a.setAccountType(AccountType.SAVINGS);
        a.setBalance(new BigDecimal("10000"));
        a.setActive(true);
        return accountRepository.save(a);
    }

    private Transaction newTx(Account a, BigDecimal amount, String type) {
        Transaction t = new Transaction();
        t.setAccount(a);
        t.setAmount(amount);
        t.setType(type);
        t.setTimestamp(LocalDateTime.now());
        t.setStatus("SUCCESS");
        return transactionRepository.save(t);
    }

    @Test
    void findByAccount_returnsOnlyThatAccountsTransactions() {
        User u = newUser("neha");
        Account a1 = newAccount(u, "ACC-111");
        Account a2 = newAccount(u, "ACC-222");

        newTx(a1, new BigDecimal("-500"), "DEBIT");
        newTx(a1, new BigDecimal("1000"), "CREDIT");
        newTx(a2, new BigDecimal("-200"), "DEBIT");

        List<Transaction> forA1 = transactionRepository.findByAccount(a1);
        assertEquals(2, forA1.size());
        assertTrue(forA1.stream().allMatch(t -> "ACC-111".equals(t.getAccount().getAccountNumber())));
    }
}

