package com.obs.service.Impl;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfGenerationServiceTest {

    private final PdfGenerationService service = new PdfGenerationService();

    private Account makeAccount(String accNum, String fullName, AccountType type, BigDecimal balance) {
        Account a = new Account();
        a.setAccountNumber(accNum);
        a.setAccountType(type);
        a.setBalance(balance);
        User u = new User();
        u.setFullName(fullName);
        a.setUser(u);
        return a;
    }

    private Transaction makeTransaction(long id, BigDecimal amount, String type, String desc, Account account, String target, String status) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAmount(amount);
        t.setTimestamp(LocalDateTime.now());
        t.setType(type);
        t.setDescription(desc);
        t.setAccount(account);
        t.setTargetAccountNumber(target);
        t.setStatus(status);
        return t;
    }

    @Test
    @DisplayName("generateTransactionInvoice - full transaction produces non-empty PDF bytes")
    void generateTransactionInvoice_full() {
        Account acc = makeAccount("ACC100", "John Doe", AccountType.SAVINGS, new BigDecimal("1000"));
        Transaction t = makeTransaction(1L, new BigDecimal("123.45"), "DEBIT", "Invoice payment", acc, "TGT123", "SUCCESS");

        byte[] pdf = service.generateTransactionInvoice(t);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0, "PDF output should not be empty");

        // Quick sanity: PDF files start with %PDF
        String header = new String(pdf, 0, Math.min(4, pdf.length));
        assertTrue(header.contains("%PDF"), "Generated content should look like a PDF");
    }

    @Test
    @DisplayName("generateTransactionInvoice - missing optional fields handled (no account, no target, null status/description)")
    void generateTransactionInvoice_missingOptionalFields() {
        Transaction t = makeTransaction(2L, new BigDecimal("50"), "CREDIT", null, null, null, null);
        byte[] pdf = service.generateTransactionInvoice(t);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String header = new String(pdf, 0, Math.min(4, pdf.length));
        assertTrue(header.contains("%PDF"));
    }

    @Test
    @DisplayName("generateTransactionInvoice - null transaction throws RuntimeException")
    void generateTransactionInvoice_null_throws() {
        assertThrows(RuntimeException.class, () -> service.generateTransactionInvoice(null));
    }

    @Test
    @DisplayName("generateAccountStatement - multiple transactions produces non-empty PDF bytes")
    void generateAccountStatement_multiple() {
        Account acc = makeAccount("ACCT-1", "Alice", AccountType.CURRENT, new BigDecimal("5000"));
        Transaction t1 = makeTransaction(10L, new BigDecimal("100"), "DEBIT", "Pay", acc, null, "SUCCESS");
        Transaction t2 = makeTransaction(11L, new BigDecimal("200"), "CREDIT", "Refund", acc, null, null);

        byte[] pdf = service.generateAccountStatement(acc, List.of(t1, t2));
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String header = new String(pdf, 0, Math.min(4, pdf.length));
        assertTrue(header.contains("%PDF"));
    }

    @Test
    @DisplayName("generateAccountStatement - empty transactions list still generates PDF")
    void generateAccountStatement_emptyTransactions() {
        Account acc = makeAccount("ACCT-2", "Bob", AccountType.SAVINGS, new BigDecimal("0"));
        byte[] pdf = service.generateAccountStatement(acc, List.of());
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String header = new String(pdf, 0, Math.min(4, pdf.length));
        assertTrue(header.contains("%PDF"));
    }

    @Test
    @DisplayName("generateAccountStatement - null account throws RuntimeException")
    void generateAccountStatement_nullAccount_throws() {
        assertThrows(RuntimeException.class, () -> service.generateAccountStatement(null, List.of()));
    }
}

