package com.obs.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class TransactionTest {

    @Test
    @DisplayName("default state: all fields null and transactionId null")
    void defaultState() {
        Transaction t = new Transaction();
        assertNull(t.getId());
        assertNull(t.getAmount());
        assertNull(t.getTimestamp());
        assertNull(t.getType());
        assertNull(t.getDescription());
        assertNull(t.getAccount());
        assertNull(t.getTargetAccountNumber());
        assertNull(t.getStatus());
        assertNull(t.getTransactionId(), "transactionId should be null when id is null");
    }

    @Test
    @DisplayName("transactionId formatting for small and larger ids")
    void transactionIdFormatting() {
        Transaction t = new Transaction();
        t.setId(1L);
        assertEquals("TXN00000001", t.getTransactionId());

        t.setId(12345678L);
        assertEquals("TXN12345678", t.getTransactionId());

        // larger than 8 digits - should not be truncated
        t.setId(123456789L);
        assertEquals("TXN123456789", t.getTransactionId());
    }

    @Test
    @DisplayName("transactionId formatting with negative id")
    void transactionIdNegativeId() {
        Transaction t = new Transaction();
        t.setId(-5L);
        // formatting includes sign and zero padding to width 8
        assertEquals("TXN-0000005", t.getTransactionId());
    }

    @Test
    @DisplayName("getters and setters store and return values (positive)")
    void gettersAndSetters() {
        Transaction t = new Transaction();
        t.setId(42L);
        t.setAmount(new BigDecimal("250.75"));
        LocalDateTime now = LocalDateTime.now();
        t.setTimestamp(now);
        t.setType("DEBIT");
        t.setDescription("Payment for invoice #123");

        Account acc = new Account();
        acc.setId(7L);
        t.setAccount(acc);

        t.setTargetAccountNumber("ACC999999");
        t.setStatus("SUCCESS");

        assertEquals(42L, t.getId());
        assertEquals(new BigDecimal("250.75"), t.getAmount());
        assertEquals(now, t.getTimestamp());
        assertEquals("DEBIT", t.getType());
        assertEquals("Payment for invoice #123", t.getDescription());
        assertSame(acc, t.getAccount());
        assertEquals("ACC999999", t.getTargetAccountNumber());
        assertEquals("SUCCESS", t.getStatus());
    }

    @Test
    @DisplayName("nulls and unusual values are accepted at POJO level (negative)")
    void nullsAndUnusualValues() {
        Transaction t = new Transaction();
        t.setAmount(null);
        t.setTimestamp(null);
        t.setType(null);
        t.setDescription(null);
        t.setAccount(null);
        t.setTargetAccountNumber("");
        t.setStatus(null);

        assertNull(t.getAmount());
        assertNull(t.getTimestamp());
        assertNull(t.getType());
        assertNull(t.getDescription());
        assertNull(t.getAccount());
        assertEquals("", t.getTargetAccountNumber());
        assertNull(t.getStatus());

        // negative amount allowed by POJO
        t.setAmount(new BigDecimal("-100.00"));
        assertEquals(new BigDecimal("-100.00"), t.getAmount());
    }

    @Test
    @DisplayName("reflection: account field annotations (@ManyToOne, @JoinColumn(name=\"account_id\"), @JsonIgnoreProperties)")
    void accountFieldAnnotations() throws NoSuchFieldException {
        Field f = Transaction.class.getDeclaredField("account");
        ManyToOne mto = f.getAnnotation(ManyToOne.class);
        assertNotNull(mto, "account should be annotated with @ManyToOne");

        JoinColumn jc = f.getAnnotation(JoinColumn.class);
        assertNotNull(jc, "account should have @JoinColumn");
        assertEquals("account_id", jc.name());

        JsonIgnoreProperties jip = f.getAnnotation(JsonIgnoreProperties.class);
        assertNotNull(jip, "account field should have @JsonIgnoreProperties");
        String[] props = jip.value();
        assertTrue(props.length > 0 && "transactions".equals(props[0]));
    }

    @Test
    @DisplayName("identity: two instances with same id remain distinct (no equals/hashCode override)")
    void identitySemantics() {
        Transaction a = new Transaction();
        Transaction b = new Transaction();
        a.setId(9L);
        b.setId(9L);
        assertNotSame(a, b);
        assertEquals(a.getId(), b.getId());
    }
}
