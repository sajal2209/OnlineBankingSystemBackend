
package com.obs.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserEntityTest {

    @Test
    void gettersSetters_andDefaults() {
        User u = new User();
        u.setUsername("neha");
        u.setPassword("pw");
        u.setFullName("Neha Patil");
        u.setEmail("neha@example.com");
        u.setPhoneNumber("9999999999");
        u.setRoles(Set.of(Role.CUSTOMER));

        Account a = new Account();
        a.setAccountNumber("ACC-111");
        u.setAccounts(List.of(a));

        assertEquals("neha", u.getUsername());
        assertTrue(u.isActive()); // default true
        assertEquals(1, u.getAccounts().size());
    }
}

