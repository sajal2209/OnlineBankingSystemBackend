package com.obs.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class RecurringPaymentTest {

    @Test
    @DisplayName("default state: newly created RecurringPayment has null fields")
    void defaultState() {
        RecurringPayment r = new RecurringPayment();
        assertNull(r.getId());
        assertNull(r.getAccount());
        assertNull(r.getAmount());
        assertNull(r.getTargetAccountNumber());
        assertNull(r.getFrequency());
        assertNull(r.getStartDate());
        assertNull(r.getEndDate());
        assertNull(r.getNextPaymentDate());
        assertNull(r.getStatus());
        assertNull(r.getCreatedAt());
    }

    @Test
    @DisplayName("getters and setters store and return values")
    void gettersAndSetters() {
        RecurringPayment r = new RecurringPayment();
        r.setId(55L);

        Account acc = new Account();
        acc.setId(3L);
        r.setAccount(acc);

        r.setAmount(new BigDecimal("99.99"));
        r.setTargetAccountNumber("ACC123456");
        r.setFrequency("MONTHLY");

        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 12, 31);
        LocalDate next = LocalDate.of(2025, 2, 1);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setNextPaymentDate(next);

        r.setStatus("ACTIVE");
        LocalDateTime created = LocalDateTime.now();
        r.setCreatedAt(created);

        assertEquals(55L, r.getId());
        assertSame(acc, r.getAccount());
        assertEquals(new BigDecimal("99.99"), r.getAmount());
        assertEquals("ACC123456", r.getTargetAccountNumber());
        assertEquals("MONTHLY", r.getFrequency());
        assertEquals(start, r.getStartDate());
        assertEquals(end, r.getEndDate());
        assertEquals(next, r.getNextPaymentDate());
        assertEquals("ACTIVE", r.getStatus());
        assertEquals(created, r.getCreatedAt());
    }

    @Test
    @DisplayName("class and account field annotations present and correct")
    void classAndFieldAnnotations() throws NoSuchFieldException {
        // class-level annotations
        Annotation ent = RecurringPayment.class.getAnnotation(Entity.class);
        assertNotNull(ent, "@Entity should be present on RecurringPayment");

        Table tbl = RecurringPayment.class.getAnnotation(Table.class);
        assertNotNull(tbl, "@Table should be present");
        assertEquals("recurring_payments", tbl.name());

        // account field annotations
        Field accField = RecurringPayment.class.getDeclaredField("account");
        ManyToOne mto = accField.getAnnotation(ManyToOne.class);
        assertNotNull(mto, "account field should have @ManyToOne");

        JoinColumn jc = accField.getAnnotation(JoinColumn.class);
        assertNotNull(jc, "account field should have @JoinColumn");
        assertEquals("account_id", jc.name());
    }

    @Test
    @DisplayName("POJO accepts nulls and unusual values (negative behavior testing)")
    void nullsAndInvalidRanges() {
        RecurringPayment r = new RecurringPayment();

        // negative amount and zero frequency string
        r.setAmount(new BigDecimal("-10.00"));
        r.setFrequency("");
        r.setTargetAccountNumber(null);

        LocalDate start = LocalDate.of(2025, 12, 31);
        LocalDate end = LocalDate.of(2025, 1, 1);
        // start date after end date (illogical) - POJO allows it
        r.setStartDate(start);
        r.setEndDate(end);
        r.setNextPaymentDate(null);

        assertEquals(new BigDecimal("-10.00"), r.getAmount());
        assertEquals("", r.getFrequency());
        assertNull(r.getTargetAccountNumber());
        assertEquals(start, r.getStartDate());
        assertEquals(end, r.getEndDate());
        assertNull(r.getNextPaymentDate());
    }

    @Test
    @DisplayName("identity semantics: two instances with same id remain distinct")
    void identitySemantics() {
        RecurringPayment a = new RecurringPayment();
        RecurringPayment b = new RecurringPayment();
        a.setId(1L);
        b.setId(1L);
        assertNotSame(a, b);
        assertEquals(a.getId(), b.getId());
    }

    @Test
    @DisplayName("mutability: modifying referenced Account reflects in RecurringPayment")
    void accountMutability() {
        RecurringPayment r = new RecurringPayment();
        Account acc = new Account();
        acc.setId(77L);
        r.setAccount(acc);

        // mutate account after setting
        acc.setAccountNumber("ACC777");
        assertSame(acc, r.getAccount());
        assertEquals("ACC777", r.getAccount().getAccountNumber());
    }

    @Test
    @DisplayName("edge cases: zero amount and long frequency string")
    void edgeCases() {
        RecurringPayment r = new RecurringPayment();
        r.setAmount(BigDecimal.ZERO);
        r.setFrequency("DAILY-EXTRA-LONG-FREQUENCY-NAME-TEST");
        assertEquals(BigDecimal.ZERO, r.getAmount());
        assertTrue(r.getFrequency().contains("DAILY"));
    }
}
