
package com.obs.repository;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.RecurringPayment;
import com.obs.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RecurringPaymentRepositoryDataJpaTest {

    @Autowired RecurringPaymentRepository recurringPaymentRepository;
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
        a.setBalance(new BigDecimal("5000"));
        a.setActive(true);
        return accountRepository.save(a);
    }

    private RecurringPayment newRecurring(Account a, String status, LocalDate nextDate, String freq) {
        RecurringPayment rp = new RecurringPayment();
        rp.setAccount(a);
        rp.setAmount(new BigDecimal("1000"));
        rp.setTargetAccountNumber("ACC-777");
        rp.setFrequency(freq);
        rp.setStartDate(LocalDate.now().minusDays(10));
        rp.setEndDate(LocalDate.now().plusDays(10));
        rp.setNextPaymentDate(nextDate);
        rp.setStatus(status);
        return recurringPaymentRepository.save(rp);
    }

    @Test
    void findByAccount_returnsOnlyLinkedPayments() {
        User u = newUser("neha");
        Account a1 = newAccount(u, "ACC-111");
        Account a2 = newAccount(u, "ACC-222");

        newRecurring(a1, "ACTIVE", LocalDate.now(), "DAILY");
        newRecurring(a1, "ACTIVE", LocalDate.now().plusDays(1), "DAILY");
        newRecurring(a2, "ACTIVE", LocalDate.now(), "WEEKLY");

        List<RecurringPayment> forA1 = recurringPaymentRepository.findByAccount(a1);
        assertEquals(2, forA1.size());
        assertTrue(forA1.stream().allMatch(r -> "ACC-111".equals(r.getAccount().getAccountNumber())));
    }

    @Test
    void findByStatusAndNextPaymentDateLessThanEqual_returnsDuePayments() {
        User u = newUser("neha");
        Account a = newAccount(u, "ACC-111");

        newRecurring(a, "ACTIVE", LocalDate.now().minusDays(1), "DAILY");
        newRecurring(a, "ACTIVE", LocalDate.now(), "DAILY");
        newRecurring(a, "ACTIVE", LocalDate.now().plusDays(2), "DAILY");
        newRecurring(a, "STOPPED", LocalDate.now(), "DAILY");

        List<RecurringPayment> due = recurringPaymentRepository
                .findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", LocalDate.now());

        assertEquals(2, due.size()); // only the first two ACTIVE with date <= today
        assertTrue(due.stream().allMatch(r -> "ACTIVE".equals(r.getStatus())
                && !r.getNextPaymentDate().isAfter(LocalDate.now())));
    }
}

