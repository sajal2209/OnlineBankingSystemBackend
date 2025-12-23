
package com.obs.service;

import com.obs.entity.Account;
import com.obs.entity.BillPayment;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import com.obs.repository.AccountRepository;
import com.obs.repository.BillPaymentRepository;
import com.obs.repository.TransactionRepository;
import com.obs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillPaymentServiceTest {

    @InjectMocks
    private BillPaymentService billPaymentService;

    @Mock
    private BillPaymentRepository billPaymentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private User user;
    private Account account;

    @BeforeEach
    void init() {
        user = new User();
        user.setId(1L);
        user.setUsername("neha");
        user.setFullName("Neha Patil");

        account = new Account();
        account.setAccountNumber("ACC-111");
        account.setUser(user);
        account.setBalance(new BigDecimal("5000"));
        account.setActive(true);

        lenient().when(accountRepository.save(any(Account.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(billPaymentRepository.save(any(BillPayment.class)))
                .thenAnswer(inv -> {
                    BillPayment b = inv.getArgument(0);
                    if (b.getId() == null) b.setId(10L);
                    return b;
                });
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void payBill_success_updatesBalance_andCreatesRecords() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("ACC-111")).thenReturn(Optional.of(account));

        billPaymentService.payBill(1L, "ACC-111", "Electricity Board", new BigDecimal("1200"));

        assertEquals(new BigDecimal("3800"), account.getBalance());

        ArgumentCaptor<BillPayment> bpCaptor = ArgumentCaptor.forClass(BillPayment.class);
        verify(billPaymentRepository).save(bpCaptor.capture());
        assertEquals("Electricity Board", bpCaptor.getValue().getBillerName());
        assertEquals("PAID", bpCaptor.getValue().getStatus());

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction t = txCaptor.getValue();
        assertEquals("DEBIT", t.getType());
        assertEquals(new BigDecimal("-1200"), t.getAmount());
        assertTrue(t.getDescription().contains("Bill Payment"));
    }

    @Test
    void payBill_insufficientFunds_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(accountRepository.findByAccountNumber("ACC-111")).thenReturn(Optional.of(account));

        var ex = assertThrows(IllegalArgumentException.class,
                () -> billPaymentService.payBill(1L, "ACC-111", "Internet", new BigDecimal("9000")));
        assertTrue(ex.getMessage().contains("Insufficient balance"));

        verify(transactionRepository, never()).save(any());
        verify(billPaymentRepository, never()).save(any());
    }

    @Test
    void getMyBills_returnsListFromRepo() {
        when(billPaymentRepository.findByUserId(1L)).thenReturn(List.of(new BillPayment(), new BillPayment()));
        assertEquals(2, billPaymentService.getMyBills(1L).size());
    }
}