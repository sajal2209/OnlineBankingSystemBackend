package com.obs.service.Impl;

import com.obs.entity.Account;
import com.obs.entity.BillPayment;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import com.obs.repository.AccountRepository;
import com.obs.repository.BillPaymentRepository;
import com.obs.repository.UserRepository;

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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillPaymentServiceTest {

    @Mock
    BillPaymentRepository billPaymentRepository;

    @Mock
    AccountRepository accountRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    com.obs.repository.TransactionRepository transactionRepository;

    @InjectMocks
    BillPaymentService billPaymentService;

    @Captor
    ArgumentCaptor<Account> accountCaptor;

    @Captor
    ArgumentCaptor<BillPayment> billPaymentCaptor;

    @Captor
    ArgumentCaptor<Transaction> transactionCaptor;

    private User makeUser(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private Account makeAccount(String accNum, Long userId, BigDecimal balance, boolean active) {
        Account a = new Account();
        a.setAccountNumber(accNum);
        a.setBalance(balance);
        a.setActive(active);
        User u = new User();
        u.setId(userId);
        a.setUser(u);
        return a;
    }

    @BeforeEach
    void setup() {
        // nothing special
    }

    @Test
    @DisplayName("payBill throws when user not found")
    void payBill_userNotFound() {
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billPaymentService.payBill(10L, "A1", "Electric", new BigDecimal("50")));
        assertEquals("User not found", ex.getMessage());

        verify(userRepository).findById(10L);
        verifyNoMoreInteractions(accountRepository, billPaymentRepository, transactionRepository);
    }

    @Test
    @DisplayName("payBill throws when account not found")
    void payBill_accountNotFound() {
        User user = makeUser(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("X" )).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billPaymentService.payBill(11L, "X", "Water", new BigDecimal("10")));
        assertEquals("Account not found", ex.getMessage());

        verify(userRepository).findById(11L);
        verify(accountRepository).findByAccountNumber("X");
        verifyNoMoreInteractions(billPaymentRepository, transactionRepository);
    }

    @Test
    @DisplayName("payBill throws when account does not belong to user")
    void payBill_accountNotBelong() {
        User user = makeUser(20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));

        Account acc = makeAccount("A10", 21L, new BigDecimal("500"), true);
        when(accountRepository.findByAccountNumber("A10")).thenReturn(Optional.of(acc));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billPaymentService.payBill(20L, "A10", "ISP", new BigDecimal("50")));
        assertEquals("Account does not belong to user", ex.getMessage());

        verify(accountRepository).findByAccountNumber("A10");
        verifyNoMoreInteractions(billPaymentRepository, transactionRepository);
    }

    @Test
    @DisplayName("payBill throws when insufficient balance")
    void payBill_insufficientBalance() {
        User user = makeUser(30L);
        when(userRepository.findById(30L)).thenReturn(Optional.of(user));

        Account acc = makeAccount("A20", 30L, new BigDecimal("20"), true);
        when(accountRepository.findByAccountNumber("A20")).thenReturn(Optional.of(acc));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> billPaymentService.payBill(30L, "A20", "Phone", new BigDecimal("50")));
        assertEquals("Insufficient balance", ex.getMessage());

        verify(accountRepository).findByAccountNumber("A20");
        verifyNoMoreInteractions(billPaymentRepository, transactionRepository);
    }

    @Test
    @DisplayName("payBill success reduces balance, saves billPayment and transaction")
    void payBill_success() {
        Long userId = 40L;
        User user = makeUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Account acc = makeAccount("ACC100", userId, new BigDecimal("1000"), true);
        when(accountRepository.findByAccountNumber("ACC100")).thenReturn(Optional.of(acc));

        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(billPaymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal amount = new BigDecimal("250.00");
        billPaymentService.payBill(userId, "ACC100", "Electric Co", amount);

        // account saved with deducted balance
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAcc = accountCaptor.getValue();
        assertEquals(0, savedAcc.getBalance().compareTo(new BigDecimal("750.00")));

        // billPayment saved with correct fields
        verify(billPaymentRepository).save(billPaymentCaptor.capture());
        BillPayment bp = billPaymentCaptor.getValue();
        assertEquals("Electric Co", bp.getBillerName());
        assertEquals(0, bp.getAmount().compareTo(amount));
        assertEquals("PAID", bp.getStatus());
        assertNotNull(bp.getDueDate());
        assertSame(user, bp.getUser());

        // transaction saved with negative amount and expected description/status/type
        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction tx = transactionCaptor.getValue();
        assertEquals(0, tx.getAmount().compareTo(amount.negate()));
        assertEquals("DEBIT", tx.getType());
        assertEquals("SUCCESS", tx.getStatus());
        assertTrue(tx.getDescription().contains("Electric Co"));
        assertNotNull(tx.getTimestamp());
    }

    @Test
    @DisplayName("getMyBills delegates to repository and returns list")
    void getMyBills_returnsList() {
        BillPayment b1 = new BillPayment();
        BillPayment b2 = new BillPayment();
        when(billPaymentRepository.findByUserId(5L)).thenReturn(List.of(b1, b2));

        List<BillPayment> out = billPaymentService.getMyBills(5L);
        assertEquals(2, out.size());
        assertSame(b1, out.iterator().next());
    }
}
