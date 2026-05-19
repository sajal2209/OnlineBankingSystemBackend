package com.obs.service.Interfaces;

import com.obs.entity.BillPayment;

import java.math.BigDecimal;
import java.util.List;

public interface IBillPaymentService {
    void payBill(Long userId, String fromAccountNumber, String billerName, BigDecimal amount);

    List<BillPayment> getMyBills(Long userId);
}
