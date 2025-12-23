
package com.obs.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RecurringPaymentEntityTest {

    @Test
    void gettersSetters() {
        RecurringPayment rp = new RecurringPayment();
        Account acc = new Account();
        acc.setAccountNumber("ACC-111");

        rp.setAccount(acc);
        rp.setAmount(new BigDecimal("500"));
        rp.setTargetAccountNumber("ACC-222");
        rp.setFrequency("MONTHLY");
        rp.setStartDate(LocalDate.now());
        rp.setEndDate(LocalDate.now().plusMonths(1));
        rp.setNextPaymentDate(LocalDate.now().plusDays(1));
        rp.setStatus("ACTIVE");

        assertEquals(acc, rp.getAccount());
        assertEquals("MONTHLY", rp.getFrequency());
        assertEquals("ACTIVE", rp.getStatus());
    }
}

