
package com.obs.service;

import com.obs.entity.Account;
import com.obs.entity.RecurringPayment;
import com.obs.entity.User;
import com.obs.repository.AccountRepository;
import com.obs.repository.RecurringPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringPaymentServiceTest {

    @InjectMocks
    private RecurringPaymentService recurringPaymentService;

    @Mock
    private RecurringPaymentRepository recurringPaymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionService transactionService;

    private Account account;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("neha");
        user.setFullName("Neha Patil");

        account = new Account();
        account.setAccountNumber("ACC-111");
        account.setUser(user);
        account.setBalance(new BigDecimal("20000"));
        account.setActive(true);

        lenient().when(recurringPaymentRepository.save(any(RecurringPayment.class)))
                .thenAnswer(inv -> {
                    RecurringPayment rp = inv.getArgument(0);
                    if (rp.getId() == null) rp.setId(7L);
                    return rp;
                });
    }

    @Test
    void createRecurringPayment_success() {
        when(accountRepository.findByAccountNumber("ACC-111")).thenReturn(Optional.of(account));

        RecurringPayment rp = recurringPaymentService.createRecurringPayment(
                "ACC-111",
                new BigDecimal("1000"),
                "ACC-222",
                "WEEKLY",
                LocalDate.now(),
                LocalDate.now().plusMonths(2)
        );

        assertEquals(account, rp.getAccount());
        assertEquals(LocalDate.now(), rp.getNextPaymentDate());
        assertEquals("ACTIVE", rp.getStatus());
    }

    @Test
    void getRecurringPaymentsByAccount_checksOwnership() {
        when(accountRepository.findByAccountNumber("ACC-111")).thenReturn(Optional.of(account));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> recurringPaymentService.getRecurringPaymentsByAccount("ACC-111", "otherUser"));
        assertTrue(ex.getMessage().contains("Unauthorized"));
    }

    @Test
    void stopRecurringPayment_marksStopped() {
        RecurringPayment rp = new RecurringPayment();
        rp.setId(5L);
        rp.setAccount(account);
        rp.setStatus("ACTIVE");

        when(recurringPaymentRepository.findById(5L)).thenReturn(Optional.of(rp));

        recurringPaymentService.stopRecurringPayment(5L, "neha");

        assertEquals("STOPPED", rp.getStatus());
        verify(recurringPaymentRepository).save(rp);
    }

    @Test
    void processRecurringPayments_executesDue_andAdvancesDate() {
        RecurringPayment due = new RecurringPayment();
        due.setId(10L);
        due.setAccount(account);
        due.setAmount(new BigDecimal("1500"));
        due.setTargetAccountNumber("ACC-222");
        due.setFrequency("DAILY");
        due.setStartDate(LocalDate.now().minusDays(1));
        due.setEndDate(LocalDate.now().plusDays(2));
        due.setNextPaymentDate(LocalDate.now());
        due.setStatus("ACTIVE");
        due.setCreatedAt(LocalDateTime.now().minusDays(1));

        when(recurringPaymentRepository.findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", LocalDate.now()))
                .thenReturn(List.of(due));

        doNothing().when(transactionService).executeRecurringTransfer(account, "ACC-222", new BigDecimal("1500"));
        when(recurringPaymentRepository.save(any(RecurringPayment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        recurringPaymentService.processRecurringPayments();

        assertEquals(LocalDate.now().plusDays(1), due.getNextPaymentDate());
        assertEquals("ACTIVE", due.getStatus());
        verify(transactionService).executeRecurringTransfer(account, "ACC-222", new BigDecimal("1500"));
    }


    @Test
    void processRecurringPayments_marksCompleted_afterEndDate() {
        RecurringPayment due = new RecurringPayment();
        due.setId(11L);
        due.setAccount(account);
        due.setAmount(new BigDecimal("500"));
        due.setTargetAccountNumber("ACC-222");
        due.setFrequency("MONTHLY");
        due.setStartDate(LocalDate.now().minusMonths(1));
        due.setEndDate(LocalDate.now());       // ends today
        due.setNextPaymentDate(LocalDate.now());
        due.setStatus("ACTIVE");

        when(recurringPaymentRepository.findByStatusAndNextPaymentDateLessThanEqual("ACTIVE", LocalDate.now()))
                .thenReturn(List.of(due));

        doNothing().when(transactionService).executeRecurringTransfer(account, "ACC-222", new BigDecimal("500"));

        when(recurringPaymentRepository.save(any(RecurringPayment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        recurringPaymentService.processRecurringPayments();

        assertEquals("COMPLETED", due.getStatus());
        verify(transactionService).executeRecurringTransfer(account, "ACC-222", new BigDecimal("500"));
    }

}