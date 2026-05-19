package com.obs.service.Interfaces;

import com.obs.entity.RecurringPayment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface IRecurringPaymentService {


    RecurringPayment createRecurringPayment(
            String accountNumber,
            BigDecimal amount,
            String targetAccountNumber,
            String frequency,
            LocalDate startDate,
            LocalDate endDate
    );

    List<RecurringPayment> getRecurringPaymentsByAccount(String accountNumber, String username);


    List<RecurringPayment> getRecurringPayments(String username);


    void stopRecurringPayment(Long id, String username);

    void processRecurringPayments();

}
