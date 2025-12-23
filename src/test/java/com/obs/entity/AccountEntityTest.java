
package com.obs.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountEntityTest {

    @Test
    void defaultActive_isTrue_andSettersWork() {
        Account a = new Account();
        assertTrue(a.isActive()); // default true

        a.setAccountNumber("ACC-111");
        a.setBalance(new BigDecimal("1000"));
        a.setActive(false);

        assertEquals("ACC-111", a.getAccountNumber());
        assertEquals(new BigDecimal("1000"), a.getBalance());
        assertFalse(a.isActive());
    }
}

