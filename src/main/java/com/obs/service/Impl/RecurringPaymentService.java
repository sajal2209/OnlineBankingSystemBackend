package com.obs.service.Impl;

import com.obs.entity.Account;
import com.obs.entity.RecurringPayment;
import com.obs.repository.AccountRepository;
import com.obs.repository.RecurringPaymentRepository;
import com.obs.service.Interfaces.IRecurringPaymentService;
import com.obs.service.Interfaces.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecurringPaymentService implements IRecurringPaymentService {

    @Autowired
    private RecurringPaymentRepository recurringPaymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ITransactionService transactionService;

    @Override
    @Transactional
    public RecurringPayment createRecurringPayment(String accountNumber, BigDecimal amount, String targetAccountNumber,
            String frequency, LocalDate startDate, LocalDate endDate) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.isActive()) {
            throw new IllegalArgumentException("Source account is inactive");
        }

        Account targetAccount = accountRepository.findByAccountNumber(targetAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Target account not found"));

        if (!targetAccount.isActive()) {
            throw new IllegalArgumentException("Target account is inactive");
        }

        if (targetAccountNumber.equals(account.getAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer funds to the same account");
        }

        RecurringPayment payment = new RecurringPayment();
        payment.setAccount(account);
        payment.setAmount(amount);
        payment.setTargetAccountNumber(targetAccountNumber);
        payment.setFrequency(frequency);
        payment.setStartDate(startDate);
        payment.setEndDate(endDate);
        payment.setNextPaymentDate(startDate);
        payment.setStatus("ACTIVE");
        payment.setCreatedAt(LocalDateTime.now());

        RecurringPayment savedPayment = recurringPaymentRepository.save(payment);

        // If start date is today (or before), process immediately
        if (!savedPayment.getStartDate().isAfter(LocalDate.now())) {
            processPayment(savedPayment);
        }

        return savedPayment;
    }

    @Override
    public List<RecurringPayment> getRecurringPaymentsByAccount(String accountNumber, String username) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        return recurringPaymentRepository.findByAccount(account);
    }

    @Override
    public void stopRecurringPayment(Long id, String username) {
        RecurringPayment payment = recurringPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (!payment.getAccount().getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        payment.setStatus("STOPPED");
        recurringPaymentRepository.save(payment);
    }

    @Scheduled(cron = "0 30 6 * * ?") // 12:00 PM IST (6:30 AM UTC)
    @Transactional
    public void processRecurringPayments() {
        LocalDate today = LocalDate.now();
        List<RecurringPayment> payments = recurringPaymentRepository
                .findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", today);

        for (RecurringPayment payment : payments) {
            processPayment(payment);
        }
    }

    private void processPayment(RecurringPayment payment) {
        try {
            transactionService.executeRecurringTransfer(payment.getAccount(), payment.getTargetAccountNumber(),
                    payment.getAmount());

            // Update Next Payment Date
            LocalDate nextDate = payment.getNextPaymentDate();
            switch (payment.getFrequency()) {
                case "DAILY":
                    nextDate = nextDate.plusDays(1);
                    break;
                case "WEEKLY":
                    nextDate = nextDate.plusWeeks(1);
                    break;
                case "MONTHLY":
                    nextDate = nextDate.plusMonths(1);
                    break;
            }

            payment.setNextPaymentDate(nextDate);

            // Check End Date
            if (payment.getEndDate() != null && nextDate.isAfter(payment.getEndDate())) {
                payment.setStatus("COMPLETED");
                payment.setNextPaymentDate(null);
            }

            recurringPaymentRepository.save(payment);

        } catch (Exception e) {
            System.err.println("Failed to process recurring payment " + payment.getId() + ": " + e.getMessage());
            // Optionally disable payment or retry later
        }
    }
}
