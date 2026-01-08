package com.obs.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AccountTest {

    @Test
    @DisplayName("default active should be true")
    void defaultActiveTrue() {
        Account a = new Account();
        assertTrue(a.isActive(), "New accounts should be active by default");
    }

    @Test
    @DisplayName("setters and getters should store and return values")
    void gettersAndSetters() {
        Account a = new Account();

        a.setId(42L);
        a.setAccountNumber("ACC123456");
        a.setAccountType(AccountType.CURRENT);
        a.setBalance(new BigDecimal("1500.50"));
        a.setActive(false);
        a.setBusinessName("Acme Inc");
        a.setBusinessAddress("1 Acme Way");
        a.setPanCardNumber("ABCDE1234F");

        User user = new User();
        user.setId(7L);
        a.setUser(user);

        Transaction t1 = new Transaction();
        t1.setId(1L);
        Transaction t2 = new Transaction();
        t2.setId(2L);

        List<Transaction> txs = new ArrayList<>();
        txs.add(t1);
        txs.add(t2);
        a.setTransactions(txs);

        assertEquals(42L, a.getId());
        assertEquals("ACC123456", a.getAccountNumber());
        assertEquals(AccountType.CURRENT, a.getAccountType());
        assertEquals(new BigDecimal("1500.50"), a.getBalance());
        assertFalse(a.isActive());
        assertEquals("Acme Inc", a.getBusinessName());
        assertEquals("1 Acme Way", a.getBusinessAddress());
        assertEquals("ABCDE1234F", a.getPanCardNumber());
        assertSame(user, a.getUser());
        assertNotNull(a.getTransactions());
        assertEquals(2, a.getTransactions().size());
        assertEquals(t1, a.getTransactions().get(0));
    }

    @Test
    @DisplayName("transactions list is mutable and reflected on account")
    void transactionsMutability() {
        Account a = new Account();
        List<Transaction> txs = new ArrayList<>();
        a.setTransactions(txs);
        assertSame(txs, a.getTransactions());
        Transaction t = new Transaction();
        t.setId(5L);
        txs.add(t);
        assertEquals(1, a.getTransactions().size());
    }

    @Test
    @DisplayName("reflection: user field should have JsonIgnore and join column with name user_id and not-nullable")
    void userFieldAnnotations() throws NoSuchFieldException {
        Field userField = Account.class.getDeclaredField("user");
        assertNotNull(userField.getAnnotation(ManyToOne.class), "user should be annotated with @ManyToOne");

        JoinColumn join = userField.getAnnotation(JoinColumn.class);
        assertNotNull(join, "user should have @JoinColumn");
        assertEquals("user_id", join.name());
        assertFalse(join.nullable(), "user join column should be not nullable");

        JsonIgnore ji = userField.getAnnotation(JsonIgnore.class);
        assertNotNull(ji, "user field should be annotated with @JsonIgnore to avoid cycles");
    }

    @Test
    @DisplayName("reflection: accountNumber column should be unique and not nullable")
    void accountNumberColumnAnnotation() throws NoSuchFieldException {
        Field f = Account.class.getDeclaredField("accountNumber");
        Column col = f.getAnnotation(Column.class);
        assertNotNull(col, "accountNumber field should have @Column");
        assertTrue(col.unique(), "accountNumber should be unique");
        assertFalse(col.nullable(), "accountNumber should be not nullable");
    }

    @Test
    @DisplayName("reflection: active field should have default column definition and be not null")
    void activeColumnDefinition() throws NoSuchFieldException {
        Field f = Account.class.getDeclaredField("active");
        Column col = f.getAnnotation(Column.class);
        assertNotNull(col);
        assertFalse(col.nullable());
        assertTrue(col.columnDefinition().toLowerCase().contains("boolean"));
    }

    @Test
    @DisplayName("reflection: transactions field mapping should be OneToMany with cascade ALL and mappedBy account")
    void transactionsMapping() throws NoSuchFieldException {
        Field f = Account.class.getDeclaredField("transactions");
        OneToMany otm = f.getAnnotation(OneToMany.class);
        assertNotNull(otm, "transactions should be annotated with @OneToMany");
        assertEquals("account", otm.mappedBy());
        CascadeType[] cascade = otm.cascade();
        boolean hasAll = false;
        for (CascadeType c : cascade) if (c == CascadeType.ALL) hasAll = true;
        assertTrue(hasAll, "cascade should include ALL");
    }

    @Test
    @DisplayName("negative: setting null account number should be allowed in POJO but getter returns null")
    void nullAccountNumberAllowed() {
        Account a = new Account();
        a.setAccountNumber(null);
        assertNull(a.getAccountNumber());
    }

    @Test
    @DisplayName("negative: toggling active off then on")
    void toggleActive() {
        Account a = new Account();
        a.setActive(false);
        assertFalse(a.isActive());
        a.setActive(true);
        assertTrue(a.isActive());
    }
}

