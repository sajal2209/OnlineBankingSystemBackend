
package com.obs.service;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfGenerationServiceTest {

    private final PdfGenerationService pdfService = new PdfGenerationService();

    private Account sampleAccount() {
        User u = new User();
        u.setId(1L);
        u.setUsername("neha");
        u.setFullName("Neha Patil");

        Account a = new Account();
        a.setAccountNumber("ACC-111");
        a.setUser(u);
        a.setAccountType(AccountType.SAVINGS);
        a.setBalance(new BigDecimal("12345.67"));
        a.setActive(true);
        return a;
    }

    @Test
    void generateTransactionInvoice_returnsBytes() {
        Account a = sampleAccount();
        Transaction t = new Transaction();
        t.setId(1L); // to get a formatted TXN id if used
        t.setAccount(a);
        t.setAmount(new BigDecimal("-250.00"));
        t.setType("DEBIT");
        t.setTimestamp(LocalDateTime.now());
        t.setDescription("Bill Payment: Electricity");
        t.setStatus("SUCCESS");

        byte[] bytes = pdfService.generateTransactionInvoice(t);

        assertNotNull(bytes);
        assertTrue(bytes.length > 500); // sanity check that a PDF was generated
    }

    @Test
    void generateAccountStatement_returnsBytes() {
        Account a = sampleAccount();

        Transaction t1 = new Transaction();
        t1.setId(2L);
        t1.setAccount(a);
        t1.setAmount(new BigDecimal("-200"));
        t1.setType("DEBIT");
        t1.setTimestamp(LocalDateTime.now());
        t1.setStatus("SUCCESS");

        Transaction t2 = new Transaction();
        t2.setId(3L);
        t2.setAccount(a);
        t2.setAmount(new BigDecimal("500"));
        t2.setType("CREDIT");
        t2.setTimestamp(LocalDateTime.now());
        t2.setStatus("SUCCESS");

        byte[] bytes = pdfService.generateAccountStatement(a, List.of(t1, t2));

        assertNotNull(bytes);
        assertTrue(bytes.length > 500);
    }
}