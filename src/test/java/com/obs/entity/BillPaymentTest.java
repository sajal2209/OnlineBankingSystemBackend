package com.obs.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class BillPaymentTest {

    @Test
    @DisplayName("default state: all fields null or default")
    void defaultState() {
        BillPayment b = new BillPayment();
        assertNull(b.getId());
        assertNull(b.getBillerName());
        assertNull(b.getAmount());
        assertNull(b.getDueDate());
        assertNull(b.getStatus());
        assertNull(b.getUser());
    }

    @Test
    @DisplayName("positive: setters and getters should store and return values")
    void settersAndGetters() {
        BillPayment b = new BillPayment();
        b.setId(101L);
        b.setBillerName("Electric Co");
        b.setAmount(new BigDecimal("123.45"));
        LocalDateTime now = LocalDateTime.now();
        b.setDueDate(now);
        b.setStatus("PENDING");

        User u = new User();
        u.setId(10L);
        u.setUsername("jdoe");
        b.setUser(u);

        assertEquals(101L, b.getId());
        assertEquals("Electric Co", b.getBillerName());
        assertEquals(new BigDecimal("123.45"), b.getAmount());
        assertEquals(now, b.getDueDate());
        assertEquals("PENDING", b.getStatus());
        assertSame(u, b.getUser());

        // mutability
        b.setBillerName("Water Co");
        assertEquals("Water Co", b.getBillerName());
    }

    @Test
    @DisplayName("reflection: user field should have @ManyToOne, @JoinColumn(name = \"user_id\") and @JsonIgnore")
    void userFieldAnnotations() throws NoSuchFieldException {
        Field userField = BillPayment.class.getDeclaredField("user");
        assertNotNull(userField.getAnnotation(ManyToOne.class), "user should be annotated with @ManyToOne");

        JoinColumn join = userField.getAnnotation(JoinColumn.class);
        assertNotNull(join, "user should have @JoinColumn");
        assertEquals("user_id", join.name(), "Join column should be named user_id");

        JsonIgnore ji = userField.getAnnotation(JsonIgnore.class);
        assertNotNull(ji, "user field should be annotated with @JsonIgnore");
    }

    @Test
    @DisplayName("negative: class allows nulls and unusual values (POJO-level behaviour)")
    void negativeNullsAndInvalidValues() {
        BillPayment b = new BillPayment();

        // nulling previously set values
        b.setBillerName(null);
        b.setAmount(null);
        b.setDueDate(null);
        b.setStatus(null);
        b.setUser(null);

        assertNull(b.getBillerName());
        assertNull(b.getAmount());
        assertNull(b.getDueDate());
        assertNull(b.getStatus());
        assertNull(b.getUser());

        // negative amount (the entity doesn't validate domain rules itself)
        b.setAmount(new BigDecimal("-50.00"));
        assertEquals(new BigDecimal("-50.00"), b.getAmount());

        // arbitrary status values are accepted
        b.setStatus("UNKNOWN_STATUS");
        assertEquals("UNKNOWN_STATUS", b.getStatus());
    }

    @Test
    @DisplayName("edge: identity mapping behaviour between two instances")
    void identityAndSeparateInstances() {
        BillPayment a = new BillPayment();
        BillPayment b = new BillPayment();
        a.setId(1L);
        b.setId(1L);

        // since BillPayment doesn't override equals/hashCode, objects remain distinct
        assertNotSame(a, b);
        assertEquals(a.getId(), b.getId());
    }
}

