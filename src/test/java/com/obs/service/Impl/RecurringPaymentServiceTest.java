package com.obs.service.Impl;

import com.obs.entity.Account;
import com.obs.entity.RecurringPayment;
import com.obs.entity.User;
import com.obs.repository.AccountRepository;
import com.obs.repository.RecurringPaymentRepository;
import com.obs.service.Interfaces.ITransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringPaymentServiceTest {

    @Mock
    RecurringPaymentRepository recurringPaymentRepository;

    @Mock
    AccountRepository accountRepository;

    @Mock
    ITransactionService transactionService;

    @InjectMocks
    RecurringPaymentService recurringPaymentService;

    @Captor
    ArgumentCaptor<RecurringPayment> paymentCaptor;

    private Account makeAccount(String accNum, String username) {
        Account a = new Account();
        a.setAccountNumber(accNum);
        User u = new User();
        u.setUsername(username);
        a.setUser(u);
        return a;
    }

    private RecurringPayment makePayment(Long id, Account account, BigDecimal amount, String target, String frequency, LocalDate nextDate, LocalDate endDate) {
        RecurringPayment p = new RecurringPayment();
        p.setId(id);
        p.setAccount(account);
        p.setAmount(amount);
        p.setTargetAccountNumber(target);
        p.setFrequency(frequency);
        p.setNextPaymentDate(nextDate);
        p.setEndDate(endDate);
        p.setStatus("ACTIVE");
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    @BeforeEach
    void before() {
        // nothing
    }

    @Test
    @DisplayName("createRecurringPayment throws when account not found")
    void createRecurringPayment_accountNotFound() {
        when(accountRepository.findByAccountNumber("X" )).thenReturn(Optional.empty());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recurringPaymentService.createRecurringPayment("X", new BigDecimal("10"), "T", "DAILY", LocalDate.now(), null));
        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    @DisplayName("createRecurringPayment creates and saves payment")
    void createRecurringPayment_success() {
        Account a = makeAccount("A1", "owner");
        when(accountRepository.findByAccountNumber("A1")).thenReturn(Optional.of(a));
        when(recurringPaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LocalDate start = LocalDate.of(2025, 12, 29);
        RecurringPayment p = recurringPaymentService.createRecurringPayment("A1", new BigDecimal("12.34"), "TGT", "WEEKLY", start, null);

        assertSame(a, p.getAccount());
        assertEquals(new BigDecimal("12.34"), p.getAmount());
        assertEquals("TGT", p.getTargetAccountNumber());
        assertEquals("WEEKLY", p.getFrequency());
        assertEquals(start, p.getNextPaymentDate());
        assertEquals("ACTIVE", p.getStatus());
        assertNotNull(p.getCreatedAt());

        verify(recurringPaymentRepository).save(any());
    }

    @Test
    @DisplayName("getRecurringPaymentsByAccount throws when account not found")
    void getRecurringPaymentsByAccount_accountNotFound() {
        when(accountRepository.findByAccountNumber("NO" )).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> recurringPaymentService.getRecurringPaymentsByAccount("NO", "u"));
    }

    @Test
    @DisplayName("getRecurringPaymentsByAccount throws when unauthorized")
    void getRecurringPaymentsByAccount_unauthorized() {
        Account a = makeAccount("A2", "owner");
        when(accountRepository.findByAccountNumber("A2")).thenReturn(Optional.of(a));
        assertThrows(IllegalArgumentException.class, () -> recurringPaymentService.getRecurringPaymentsByAccount("A2", "intruder"));
    }

    @Test
    @DisplayName("getRecurringPaymentsByAccount returns list on success")
    void getRecurringPaymentsByAccount_success() {
        Account a = makeAccount("A3", "owner");
        RecurringPayment p1 = makePayment(1L, a, new BigDecimal("5"), "T", "DAILY", LocalDate.now(), null);
        when(accountRepository.findByAccountNumber("A3")).thenReturn(Optional.of(a));
        when(recurringPaymentRepository.findByAccount(a)).thenReturn(List.of(p1));

        List<RecurringPayment> out = recurringPaymentService.getRecurringPaymentsByAccount("A3", "owner");
        assertEquals(1, out.size());
        assertSame(p1, out.get(0));
    }

    @Test
    @DisplayName("stopRecurringPayment throws when not found")
    void stopRecurringPayment_notFound() {
        when(recurringPaymentRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> recurringPaymentService.stopRecurringPayment(99L, "u"));
    }

    @Test
    @DisplayName("stopRecurringPayment throws when unauthorized")
    void stopRecurringPayment_unauthorized() {
        Account a = makeAccount("A4", "owner");
        RecurringPayment p = makePayment(2L, a, new BigDecimal("1"), "T", "DAILY", LocalDate.now(), null);
        when(recurringPaymentRepository.findById(2L)).thenReturn(Optional.of(p));
        assertThrows(IllegalArgumentException.class, () -> recurringPaymentService.stopRecurringPayment(2L, "other"));
    }

    @Test
    @DisplayName("stopRecurringPayment sets status STOPPED and saves")
    void stopRecurringPayment_success() {
        Account a = makeAccount("A5", "owner");
        RecurringPayment p = makePayment(3L, a, new BigDecimal("1"), "T", "DAILY", LocalDate.now(), null);
        when(recurringPaymentRepository.findById(3L)).thenReturn(Optional.of(p));
        when(recurringPaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recurringPaymentService.stopRecurringPayment(3L, "owner");

        verify(recurringPaymentRepository).save(paymentCaptor.capture());
        RecurringPayment saved = paymentCaptor.getValue();
        assertEquals("STOPPED", saved.getStatus());
    }

    @Test
    @DisplayName("processRecurringPayments updates nextDate for DAILY/WEEKLY/MONTHLY and completes when past endDate")
    void processRecurringPayments_updatesAndCompletes() {
        LocalDate today = LocalDate.now();
        Account a = makeAccount("AA", "u");
        // for DAILY: nextDate -> today +1 -> after endDate -> COMPLETED
        RecurringPayment daily = makePayment(10L, a, new BigDecimal("1"), "T", "DAILY", today, today);
        // for WEEKLY: nextDate -> today +1 week -> not after endDate (set big endDate)
        RecurringPayment weekly = makePayment(11L, a, new BigDecimal("2"), "T", "WEEKLY", today, today.plusWeeks(10));
        // for MONTHLY: nextDate -> today +1 month -> not after endDate
        RecurringPayment monthly = makePayment(12L, a, new BigDecimal("3"), "T", "MONTHLY", today, today.plusMonths(2));

        when(recurringPaymentRepository.findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", today)).thenReturn(List.of(daily, weekly, monthly));
        doNothing().when(transactionService).executeRecurringTransfer(any(), anyString(), any());
        when(recurringPaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recurringPaymentService.processRecurringPayments();

        // daily completed
        assertEquals("COMPLETED", daily.getStatus());
        // weekly & monthly remain ACTIVE
        assertEquals("ACTIVE", weekly.getStatus());
        assertEquals("ACTIVE", monthly.getStatus());

        // nextPaymentDate advanced appropriately
        assertEquals(today.plusDays(1), daily.getNextPaymentDate());
        assertEquals(today.plusWeeks(1), weekly.getNextPaymentDate());
        assertEquals(today.plusMonths(1), monthly.getNextPaymentDate());

        verify(recurringPaymentRepository, times(3)).save(any());
    }

    @Test
    @DisplayName("processRecurringPayments skips saving when transactionService throws")
    void processRecurringPayments_transactionThrows() {
        LocalDate today = LocalDate.now();
        Account a = makeAccount("AB", "u");
        RecurringPayment bad = makePayment(20L, a, new BigDecimal("5"), "T", "DAILY", today, null);

        when(recurringPaymentRepository.findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", today)).thenReturn(List.of(bad));
        doThrow(new RuntimeException("boom")).when(transactionService).executeRecurringTransfer(any(), anyString(), any());

        recurringPaymentService.processRecurringPayments();

        // since transaction threw, save should not be called
        verify(recurringPaymentRepository, never()).save(any());
        // but status remains ACTIVE
        assertEquals("ACTIVE", bad.getStatus());
    }

    @Test
    @DisplayName("processRecurringPayments handles unknown frequency by leaving nextDate unchanged")
    void processRecurringPayments_unknownFrequency() {
        LocalDate today = LocalDate.now();
        Account a = makeAccount("AC", "u");
        RecurringPayment p = makePayment(30L, a, new BigDecimal("7"), "T", "YEARLY", today, null);

        when(recurringPaymentRepository.findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", today)).thenReturn(List.of(p));
        doNothing().when(transactionService).executeRecurringTransfer(any(), anyString(), any());
        when(recurringPaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        recurringPaymentService.processRecurringPayments();

        // nextDate should remain equal to original + still saved
        assertEquals(today, p.getNextPaymentDate());
        verify(recurringPaymentRepository).save(any());
    }
}

