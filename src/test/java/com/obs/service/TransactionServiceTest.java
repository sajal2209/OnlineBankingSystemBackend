
package com.obs.service;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import com.obs.payload.request.TransferRequest;
import com.obs.repository.AccountRepository;
import com.obs.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private Account from;
    private Account to;

    @BeforeEach
    void setUp() {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("neha");
        u1.setFullName("Neha Patil");

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("payee");
        u2.setFullName("Payee User");

        from = new Account();
        from.setAccountNumber("ACC-111");
        from.setUser(u1);
        from.setBalance(new BigDecimal("15000"));
        from.setAccountType(AccountType.SAVINGS);
        from.setActive(true);

        to = new Account();
        to.setAccountNumber("ACC-222");
        to.setUser(u2);
        to.setBalance(new BigDecimal("1000"));
        to.setAccountType(AccountType.SAVINGS);
        to.setActive(true);

        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(1L); // ensure getTransactionId() works if needed
                    }
                    if (t.getTimestamp() == null) {
                        t.setTimestamp(LocalDateTime.now());
                    }
                    return t;
                });
    }

    private TransferRequest req(String fromAcc, String toAcc, String amount) {
        TransferRequest r = new TransferRequest();
        r.setFromAccountNumber(fromAcc);
        r.setToAccountNumber(toAcc);
        r.setAmount(new BigDecimal(amount));
        return r;
    }

    @Test
    void transferFunds_success_underLimit() {
        when(accountRepository.findByAccountNumber("ACC-111")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("ACC-222")).thenReturn(Optional.of(to));

        String status = transactionService.transferFunds(req("ACC-111", "ACC-222", "5000"), "neha");

        assertEquals("SUCCESS", status);
        assertEquals(new BigDecimal("10000"), from.getBalance()); // 15000 - 5000
        assertEquals(new BigDecimal("6000"), to.getBalance());    // 1000 + 5000

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, times(2)).save(txCaptor.capture());
        var types = txCaptor.getAllValues().stream().map(Transaction::getType).toList();
        assertTrue(types.contains("DEBIT"));
        assertTrue(types.contains("CREDIT"));
    }

    @Test
    void transferFunds_pending_whenOverLimit_andNotCurrent() {
        when(accountRepository.findByAccountNumber("ACC-111")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("ACC-222")).thenReturn(Optional.of(to));

        String status = transactionService.transferFunds(req("ACC-111", "ACC-222", "12000"), "neha");

        assertEquals("PENDING", status);
        assertEquals(new BigDecimal("3000"), from.getBalance()); // held funds
        assertEquals(new BigDecimal("1000"), to.getBalance());   // not credited yet

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction pending = txCaptor.getValue();
        assertEquals("PENDING", pending.getStatus());
        assertEquals(new BigDecimal("-12000"), pending.getAmount());
        assertEquals("ACC-222", pending.getTargetAccountNumber());
    }

    @Test
    void approveTransaction_movesFunds_andMarksSuccess() {
        Transaction pending = new Transaction();
        pending.setId(10L);
        pending.setAccount(from);
        pending.setAmount(new BigDecimal("-12000"));
        pending.setType("DEBIT");
        pending.setTargetAccountNumber("ACC-222");
        pending.setStatus("PENDING");
        pending.setDescription("Transfer to ACC-222 (PENDING APPROVAL)");

        from.setBalance(new BigDecimal("3000")); // after hold

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(accountRepository.findByAccountNumber("ACC-222")).thenReturn(Optional.of(to));

        transactionService.approveTransaction(1L);

        assertEquals(new BigDecimal("13000"), to.getBalance()); // 1000 + 12000
        assertEquals("SUCCESS", pending.getStatus());
        assertFalse(pending.getDescription().contains("PENDING"));

        verify(transactionRepository, atLeast(1)).save(any(Transaction.class)); // includes receiver credit
    }

    @Test
    void rejectTransaction_refundsSource_andMarksRejected() {
        Transaction pending = new Transaction();
        pending.setId(20L);
        pending.setAccount(from);
        pending.setAmount(new BigDecimal("-8000"));
        pending.setType("DEBIT");
        pending.setTargetAccountNumber("ACC-222");
        pending.setStatus("PENDING");

        from.setBalance(new BigDecimal("7000")); // after hold

        when(transactionRepository.findById(2L)).thenReturn(Optional.of(pending));

        transactionService.rejectTransaction(2L);

        assertEquals(new BigDecimal("15000"), from.getBalance()); // refunded
        assertEquals("REJECTED", pending.getStatus());
        verify(transactionRepository).save(pending);
    }

    @Test
    void executeRecurringTransfer_success() {
        when(accountRepository.findByAccountNumber("ACC-222")).thenReturn(Optional.of(to));

        transactionService.executeRecurringTransfer(from, "ACC-222", new BigDecimal("2000"));

        assertEquals(new BigDecimal("13000"), from.getBalance());
        assertEquals(new BigDecimal("3000"), to.getBalance());

        verify(transactionRepository, times(2)).save(any(Transaction.class)); // debit + credit
    }

    @Test
    void transferFunds_throwsIfNotOwner() {
        when(accountRepository.findByAccountNumber("ACC-111")).thenReturn(Optional.of(from));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> transactionService.transferFunds(req("ACC-111", "ACC-222", "1000"), "wrongUser"));
        assertTrue(ex.getMessage().contains("do not own"));
    }
}
