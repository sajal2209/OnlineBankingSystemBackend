
package com.obs.repository;

import com.obs.entity.BillPayment;
import com.obs.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class BillPaymentRepositoryDataJpaTest {

    @Autowired BillPaymentRepository billPaymentRepository;
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

    private BillPayment newBill(User u, String biller, BigDecimal amount, String status) {
        BillPayment bp = new BillPayment();
        bp.setUser(u);
        bp.setBillerName(biller);
        bp.setAmount(amount);
        bp.setDueDate(LocalDateTime.now());
        bp.setStatus(status);
        return billPaymentRepository.save(bp);
    }

    @Test
    void findByUserId_returnsBillsForThatUser() {
        User u1 = newUser("neha");
        User u2 = newUser("payee");

        newBill(u1, "Electricity", new BigDecimal("1200"), "PAID");
        newBill(u1, "Internet", new BigDecimal("800"), "PAID");
        newBill(u2, "Water", new BigDecimal("500"), "PAID");

        List<BillPayment> bills = billPaymentRepository.findByUserId(u1.getId());
        assertEquals(2, bills.size());
        assertTrue(bills.stream().allMatch(b -> b.getUser().getId().equals(u1.getId())));
    }
}

