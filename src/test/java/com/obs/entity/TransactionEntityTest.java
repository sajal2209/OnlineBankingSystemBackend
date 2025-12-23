
package com.obs.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionEntityTest {

    @Test
    void transactionId_formatsWithPrefix() {
        Transaction t = new Transaction();
        assertNull(t.getTransactionId());
        t.setId(42L);
        assertEquals("TXN00000042", t.getTransactionId());
    }

    @Test
    void settersWork() {
        Transaction t = new Transaction();
        t.setAmount(new BigDecimal("-100"));
        t.setType("DEBIT");
        t.setTimestamp(LocalDateTime.now());
        t.setDescription("Test");
        t.setTargetAccountNumber("ACC-222");
        t.setStatus("SUCCESS");

        assertEquals("DEBIT", t.getType());
        assertEquals(new BigDecimal("-100"), t.getAmount());
        assertEquals("ACC-222", t.getTargetAccountNumber());
        assertEquals("SUCCESS", t.getStatus());
    }
}

