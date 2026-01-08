package com.obs.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserTest {

    @Test
    @DisplayName("default active should be true")
    void defaultActiveTrue() {
        User u = new User();
        assertTrue(u.isActive(), "New users should be active by default");
    }

    @Test
    @DisplayName("setters and getters store and return values")
    void gettersAndSetters() {
        User u = new User();
        u.setId(10L);
        u.setUsername("bob");
        u.setPassword("p@ssw0rd");
        u.setEmail("bob@example.com");
        u.setPhoneNumber("+911234567890");
        u.setActive(false);
        u.setFullName("Bob Builder");

        Set<Role> roles = new HashSet<>();
        roles.add(Role.CUSTOMER);
        roles.add(Role.BANKER);
        u.setRoles(roles);

        Account a1 = new Account();
        a1.setId(1L);
        Account a2 = new Account();
        a2.setId(2L);
        List<Account> accounts = new ArrayList<>();
        accounts.add(a1);
        accounts.add(a2);
        u.setAccounts(accounts);

        assertEquals(10L, u.getId());
        assertEquals("bob", u.getUsername());
        assertEquals("p@ssw0rd", u.getPassword());
        assertEquals("bob@example.com", u.getEmail());
        assertEquals("+911234567890", u.getPhoneNumber());
        assertFalse(u.isActive());
        assertEquals("Bob Builder", u.getFullName());
        assertSame(roles, u.getRoles());
        assertSame(accounts, u.getAccounts());
        assertEquals(2, u.getAccounts().size());
    }

    @Test
    @DisplayName("toggle active works")
    void toggleActive() {
        User u = new User();
        u.setActive(false);
        assertFalse(u.isActive());
        u.setActive(true);
        assertTrue(u.isActive());
    }

    @Test
    @DisplayName("id field has sequence generator and GeneratedValue")
    void idFieldAnnotations() throws NoSuchFieldException {
        Field idField = User.class.getDeclaredField("id");
        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(gv, "@GeneratedValue should be present");
        assertEquals(GenerationType.SEQUENCE, gv.strategy());
        assertEquals("user_seq", gv.generator());

        jakarta.persistence.SequenceGenerator sg = idField.getAnnotation(jakarta.persistence.SequenceGenerator.class);
        assertNotNull(sg, "@SequenceGenerator should be present");
        assertEquals("user_seq", sg.name());
        assertEquals("obs_user_seq", sg.sequenceName());
        assertEquals(100000001, sg.initialValue());
        assertEquals(1, sg.allocationSize());
    }

    @Test
    @DisplayName("username column constraints: unique & not nullable")
    void usernameColumnConstraints() throws NoSuchFieldException {
        Field f = User.class.getDeclaredField("username");
        Column col = f.getAnnotation(Column.class);
        assertNotNull(col);
        assertTrue(col.unique());
        assertFalse(col.nullable());
    }

    @Test
    @DisplayName("password field annotations: JsonIgnore and not nullable")
    void passwordAnnotations() throws NoSuchFieldException {
        Field f = User.class.getDeclaredField("password");
        Column col = f.getAnnotation(Column.class);
        assertNotNull(col);
        assertFalse(col.nullable());

        JsonIgnore ji = f.getAnnotation(JsonIgnore.class);
        assertNotNull(ji, "password should be annotated with @JsonIgnore");
    }

    @Test
    @DisplayName("roles field mapping: ElementCollection and Enumerated STRING")
    void rolesFieldAnnotations() throws NoSuchFieldException {
        Field f = User.class.getDeclaredField("roles");
        ElementCollection ec = f.getAnnotation(ElementCollection.class);
        assertNotNull(ec);
        assertEquals(FetchType.EAGER, ec.fetch());

        Enumerated en = f.getAnnotation(Enumerated.class);
        assertNotNull(en);
        assertEquals(EnumType.STRING, en.value());
    }

    @Test
    @DisplayName("accounts mapping: OneToMany mappedBy=user and cascade ALL with JsonIgnore")
    void accountsFieldAnnotations() throws NoSuchFieldException {
        Field f = User.class.getDeclaredField("accounts");
        OneToMany otm = f.getAnnotation(OneToMany.class);
        assertNotNull(otm);
        assertEquals("user", otm.mappedBy());
        boolean hasAll = false;
        for (CascadeType c : otm.cascade()) {
            if (c == CascadeType.ALL) { hasAll = true; break; }
        }
        assertTrue(hasAll, "cascade should include ALL");

        JsonIgnore ji = f.getAnnotation(JsonIgnore.class);
        assertNotNull(ji, "accounts should be annotated with JsonIgnore");
    }

    @Test
    @DisplayName("POJO allows null username (DB-level constraint not enforced in POJO)")
    void nullUsernameAllowed() {
        User u = new User();
        u.setUsername(null);
        assertNull(u.getUsername());
    }

    @Test
    @DisplayName("identity semantics: two instances with same id are distinct (no equals/ hashCode)")
    void identityWithoutEquals() {
        User a = new User();
        User b = new User();
        a.setId(7L);
        b.setId(7L);
        assertNotSame(a, b);
        assertEquals(a.getId(), b.getId());
    }

    @Test
    @DisplayName("roles mutability is reflected on the user object")
    void rolesMutability() {
        User u = new User();
        Set<Role> roles = EnumSet.of(Role.CUSTOMER);
        u.setRoles(roles);
        assertSame(roles, u.getRoles());
        roles.add(Role.ADMIN);
        assertTrue(u.getRoles().contains(Role.ADMIN));
    }
}
