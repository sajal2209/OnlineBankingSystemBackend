
package com.obs.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class BillPaymentEntityTest {

    @Test
    void gettersSetters() {
        BillPayment bp = new BillPayment();
        bp.setBillerName("Electricity");
        bp.setAmount(new BigDecimal("1200"));
        bp.setDueDate(LocalDateTime.now());
        bp.setStatus("PAID");

        assertEquals("Electricity", bp.getBillerName());
        assertEquals(new BigDecimal("1200"), bp.getAmount());
        assertEquals("PAID", bp.getStatus());
    }
}

