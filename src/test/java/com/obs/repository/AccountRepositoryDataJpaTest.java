
package com.obs.repository;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AccountRepositoryDataJpaTest {

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

    private Account newAccount(User user, String accNo, BigDecimal balance, AccountType type) {
        Account a = new Account();
        a.setAccountNumber(accNo);
        a.setUser(user);
        a.setBalance(balance);
        a.setAccountType(type);
        a.setActive(true);
        return accountRepository.save(a);
    }

    @Test
    void persistAndFindByAccountNumber() {
        User u = newUser("neha");
        newAccount(u, "ACC-111", new BigDecimal("1000"), AccountType.SAVINGS);

        Optional<Account> found = accountRepository.findByAccountNumber("ACC-111");
        assertTrue(found.isPresent());
        assertEquals("neha", found.get().getUser().getUsername());
    }

    @Test
    void findByUser_returnsAllUserAccounts() {
        User u = newUser("neha");
        newAccount(u, "ACC-111", new BigDecimal("1000"), AccountType.SAVINGS);
        newAccount(u, "ACC-222", new BigDecimal("5000"), AccountType.CURRENT);

        List<Account> accounts = accountRepository.findByUser(u);
        assertEquals(2, accounts.size());
    }

    @Test
    void uniqueAccountNumber_enforced() {
        User u = newUser("neha");
        newAccount(u, "ACC-999", new BigDecimal("1000"), AccountType.SAVINGS);

        Account duplicate = new Account();
        duplicate.setAccountNumber("ACC-999"); // same
        duplicate.setUser(u);
        duplicate.setBalance(new BigDecimal("200"));
        duplicate.setAccountType(AccountType.SAVINGS);

        assertThrows(DataIntegrityViolationException.class, () -> accountRepository.saveAndFlush(duplicate));
    }
}

