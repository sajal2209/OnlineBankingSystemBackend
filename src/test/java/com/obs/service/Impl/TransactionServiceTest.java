package com.obs.service.Impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import com.obs.payload.request.TransferRequest;
import com.obs.repository.AccountRepository;
import com.obs.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    TransactionRepository transactionRepository;

    @InjectMocks
    TransactionService transactionService;

    @Captor
    ArgumentCaptor<Account> accountCaptor;

    @Captor
    ArgumentCaptor<Transaction> transactionCaptor;

    private Account makeAccount(String acctNum, String username, BigDecimal balance, boolean active, AccountType type) {
        Account a = new Account();
        a.setAccountNumber(acctNum);
        a.setBalance(balance);
        a.setActive(active);
        a.setAccountType(type);
        User u = new User();
        u.setUsername(username);
        a.setUser(u);
        return a;
    }

    @BeforeEach
    void before() {
        // nothing
    }

    // ---------------- transferFunds branches ----------------

    @Test
    @DisplayName("transferFunds - success automatic approval (small amount)")
    void transferFunds_success() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("A1");
        req.setToAccountNumber("A2");
        req.setAmount(new BigDecimal("500"));

        Account from = makeAccount("A1", "alice", new BigDecimal("1000"), true, AccountType.SAVINGS);
        Account to = makeAccount("A2", "bob", new BigDecimal("200"), true, AccountType.SAVINGS);

        when(accountRepository.findByAccountNumber("A1")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("A2")).thenReturn(Optional.of(to));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String result = transactionService.transferFunds(req, "alice");
        assertEquals("SUCCESS", result);

        // balances updated
        verify(accountRepository, atLeastOnce()).save(accountCaptor.capture());
        List<Account> savedAccounts = accountCaptor.getAllValues();
        boolean fromSaved = savedAccounts.stream().anyMatch(a -> a.getAccountNumber().equals("A1") && a.getBalance().compareTo(new BigDecimal("500"))==0);
        boolean toSaved = savedAccounts.stream().anyMatch(a -> a.getAccountNumber().equals("A2") && a.getBalance().compareTo(new BigDecimal("700"))==0);
        assertTrue(fromSaved && toSaved);

        // transactions saved (debit + credit)
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
        List<Transaction> txs = transactionCaptor.getAllValues();
        assertTrue(txs.stream().anyMatch(t -> "DEBIT".equals(t.getType()) && t.getAmount().compareTo(new BigDecimal("-500"))==0));
        assertTrue(txs.stream().anyMatch(t -> "CREDIT".equals(t.getType()) && t.getAmount().compareTo(new BigDecimal("500"))==0));
    }

    @Test
    @DisplayName("transferFunds - pending for high value when from is not CURRENT")
    void transferFunds_pendingHighValue() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("A3");
        req.setToAccountNumber("A4");
        req.setAmount(new BigDecimal("200000"));

        Account from = makeAccount("A3", "carol", new BigDecimal("300000"), true, AccountType.SAVINGS);
        Account to = makeAccount("A4", "dave", new BigDecimal("1000"), true, AccountType.SAVINGS);

        when(accountRepository.findByAccountNumber("A3")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("A4")).thenReturn(Optional.of(to));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String res = transactionService.transferFunds(req, "carol");
        assertEquals("PENDING", res);

        // source balance deducted
        verify(accountRepository).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertEquals("A3", saved.getAccountNumber());
        assertEquals(0, saved.getBalance().compareTo(new BigDecimal("100000")));

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTx = transactionCaptor.getValue();
        assertEquals("PENDING", savedTx.getStatus());
        assertEquals(0, savedTx.getAmount().compareTo(new BigDecimal("-200000")));
    }

    @Test
    @DisplayName("transferFunds - high value but from is CURRENT => automatic success")
    void transferFunds_highValueFromCurrent() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("AC");
        req.setToAccountNumber("AD");
        req.setAmount(new BigDecimal("200000"));

        Account from = makeAccount("AC", "erin", new BigDecimal("500000"), true, AccountType.CURRENT);
        Account to = makeAccount("AD", "frank", new BigDecimal("100000"), true, AccountType.SAVINGS);

        when(accountRepository.findByAccountNumber("AC")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("AD")).thenReturn(Optional.of(to));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String res = transactionService.transferFunds(req, "erin");
        assertEquals("SUCCESS", res);
        verify(transactionRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("transferFunds - source account not found")
    void transferFunds_sourceNotFound() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("NX");
        when(accountRepository.findByAccountNumber("NX")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferFunds(req, "u"));
    }

    @Test
    @DisplayName("transferFunds - target account not found")
    void transferFunds_targetNotFound() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("A1");
        req.setToAccountNumber("ZZ");
        req.setAmount(new BigDecimal("10"));
        Account from = makeAccount("A1", "sam", new BigDecimal("100"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("A1")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("ZZ")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferFunds(req, "sam"));
    }

    @Test
    @DisplayName("transferFunds - cannot transfer to same account")
    void transferFunds_sameAccount() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("S1");
        req.setToAccountNumber("S1");
        req.setAmount(new BigDecimal("10"));
        Account from = makeAccount("S1", "owner", new BigDecimal("100"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("S1")).thenReturn(Optional.of(from));

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferFunds(req, "owner"));
    }

    @Test
    @DisplayName("transferFunds - ownership mismatch")
    void transferFunds_ownershipMismatch() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("O1");
        req.setToAccountNumber("O2");
        req.setAmount(new BigDecimal("10"));
        Account from = makeAccount("O1", "auser", new BigDecimal("100"), true, AccountType.SAVINGS);
        // Account to = makeAccount("O2", "other", new BigDecimal("50"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("O1")).thenReturn(Optional.of(from));
        // when(accountRepository.findByAccountNumber("O2")).thenReturn(Optional.of(to));

        // Note: stubbing the target account is unnecessary for this test because
        // the service detects ownership mismatch after reading only the source account
        // and throws before loading the target, so we avoid stubbing it to prevent
        // UnnecessaryStubbingException from Mockito's strictness.

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferFunds(req, "someoneElse"));
    }

    @Test
    @DisplayName("transferFunds - source inactive")
    void transferFunds_sourceInactive() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("I1");
        req.setToAccountNumber("I2");
        req.setAmount(new BigDecimal("10"));
        Account from = makeAccount("I1", "ua", new BigDecimal("100"), false, AccountType.SAVINGS);
        Account to = makeAccount("I2", "ub", new BigDecimal("50"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("I1")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("I2")).thenReturn(Optional.of(to));

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferFunds(req, "ua"));
    }

    @Test
    @DisplayName("transferFunds - target inactive")
    void transferFunds_targetInactive() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("T1");
        req.setToAccountNumber("T2");
        req.setAmount(new BigDecimal("10"));
        Account from = makeAccount("T1", "ua", new BigDecimal("100"), true, AccountType.SAVINGS);
        Account to = makeAccount("T2", "ub", new BigDecimal("50"), false, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("T1")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("T2")).thenReturn(Optional.of(to));

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferFunds(req, "ua"));
    }

    @Test
    @DisplayName("transferFunds - insufficient balance")
    void transferFunds_insufficient() {
        TransferRequest req = new TransferRequest();
        req.setFromAccountNumber("B1");
        req.setToAccountNumber("B2");
        req.setAmount(new BigDecimal("1000"));
        Account from = makeAccount("B1", "u", new BigDecimal("100"), true, AccountType.SAVINGS);
        Account to = makeAccount("B2", "v", new BigDecimal("50"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("B1")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("B2")).thenReturn(Optional.of(to));

        assertThrows(IllegalArgumentException.class, () -> transactionService.transferFunds(req, "u"));
    }

    // ---------------- approveTransaction ----------------

    @Test
    @DisplayName("approveTransaction - not found")
    void approveTransaction_notFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.approveTransaction(99L));
    }

    @Test
    @DisplayName("approveTransaction - not pending")
    void approveTransaction_notPending() {
        Transaction tx = new Transaction();
        tx.setStatus("SUCCESS");
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(tx));
        assertThrows(IllegalArgumentException.class, () -> transactionService.approveTransaction(2L));
    }

    @Test
    @DisplayName("approveTransaction - target not found")
    void approveTransaction_targetNotFound() {
        Account from = makeAccount("F1", "remit", new BigDecimal("1000"), true, AccountType.SAVINGS);
        Transaction tx = new Transaction();
        tx.setStatus("PENDING");
        tx.setAccount(from);
        tx.setTargetAccountNumber("NOACC");
        when(transactionRepository.findById(3L)).thenReturn(Optional.of(tx));
        when(accountRepository.findByAccountNumber("NOACC")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.approveTransaction(3L));
    }

    @Test
    @DisplayName("approveTransaction - target inactive")
    void approveTransaction_targetInactive() {
        Account from = makeAccount("F2", "remit", new BigDecimal("1000"), true, AccountType.SAVINGS);
        Account to = makeAccount("TO", "recv", new BigDecimal("0"), false, AccountType.SAVINGS);
        Transaction tx = new Transaction();
        tx.setStatus("PENDING");
        tx.setAccount(from);
        tx.setTargetAccountNumber("TO");

        when(transactionRepository.findById(4L)).thenReturn(Optional.of(tx));
        when(accountRepository.findByAccountNumber("TO")).thenReturn(Optional.of(to));

        assertThrows(IllegalArgumentException.class, () -> transactionService.approveTransaction(4L));
    }

    @Test
    @DisplayName("approveTransaction - success")
    void approveTransaction_success() {
        Account from = makeAccount("FS", "sender", new BigDecimal("1000"), true, AccountType.SAVINGS);
        Account to = makeAccount("TS", "receiver", new BigDecimal("100"), true, AccountType.SAVINGS);
        Transaction tx = new Transaction();
        tx.setStatus("PENDING");
        tx.setAccount(from);
        tx.setAmount(new BigDecimal("200").negate());
        tx.setTargetAccountNumber("TS");
        tx.setDescription("Transfer to receiver (PENDING APPROVAL)");

        when(transactionRepository.findById(5L)).thenReturn(Optional.of(tx));
        when(accountRepository.findByAccountNumber("TS")).thenReturn(Optional.of(to));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transactionService.approveTransaction(5L);

        // to account balance increased by abs(amount)
        verify(accountRepository).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertEquals(0, saved.getBalance().compareTo(new BigDecimal("300")));

        // transaction updated to SUCCESS
        verify(transactionRepository, atLeastOnce()).save(transactionCaptor.capture());
        List<Transaction> savedTxs = transactionCaptor.getAllValues();
        assertTrue(savedTxs.stream().anyMatch(t -> "SUCCESS".equals(t.getStatus())));

        // a credit transaction was created
        assertTrue(savedTxs.stream().anyMatch(t -> "CREDIT".equals(t.getType()) && t.getAmount().compareTo(new BigDecimal("200"))==0));
    }

    // ---------------- rejectTransaction ----------------

    @Test
    @DisplayName("rejectTransaction - not found")
    void rejectTransaction_notFound() {
        when(transactionRepository.findById(77L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.rejectTransaction(77L));
    }

    @Test
    @DisplayName("rejectTransaction - not pending")
    void rejectTransaction_notPending() {
        Transaction tx = new Transaction();
        tx.setStatus("SUCCESS");
        when(transactionRepository.findById(78L)).thenReturn(Optional.of(tx));
        assertThrows(IllegalArgumentException.class, () -> transactionService.rejectTransaction(78L));
    }

    @Test
    @DisplayName("rejectTransaction - success refunds and marks rejected")
    void rejectTransaction_success() {
        Account from = makeAccount("RF", "orig", new BigDecimal("500"), true, AccountType.SAVINGS);
        Transaction tx = new Transaction();
        tx.setStatus("PENDING");
        tx.setAccount(from);
        tx.setAmount(new BigDecimal("150").negate());
        tx.setDescription("Transfer (PENDING APPROVAL)");

        when(transactionRepository.findById(79L)).thenReturn(Optional.of(tx));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transactionService.rejectTransaction(79L);

        verify(accountRepository).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertEquals(0, saved.getBalance().compareTo(new BigDecimal("650")));

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction savedTx = transactionCaptor.getValue();
        assertEquals("REJECTED", savedTx.getStatus());
        assertTrue(savedTx.getDescription().contains("REJECTED"));
    }

    // ---------------- getPendingTransactions ----------------

    @Test
    @DisplayName("getPendingTransactions filters correctly")
    void getPendingTransactions_filters() {
        Transaction t1 = new Transaction(); t1.setStatus("PENDING");
        Transaction t2 = new Transaction(); t2.setStatus("SUCCESS");
        when(transactionRepository.findAll()).thenReturn(List.of(t1, t2));

        List<Transaction> pending = transactionService.getPendingTransactions();
        assertEquals(1, pending.size());
        assertEquals("PENDING", pending.get(0).getStatus());
    }

    // ---------------- getTransactionHistory ----------------

    @Test
    @DisplayName("getTransactionHistory - account not found")
    void getTransactionHistory_accountNotFound() {
        when(accountRepository.findByAccountNumber("X" )).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.getTransactionHistory("X", "u"));
    }

    @Test
    @DisplayName("getTransactionHistory - unauthorized")
    void getTransactionHistory_unauthorized() {
        Account a = makeAccount("H1", "owner", BigDecimal.ZERO, true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("H1")).thenReturn(Optional.of(a));
        assertThrows(IllegalArgumentException.class, () -> transactionService.getTransactionHistory("H1", "intruder"));
    }

    @Test
    @DisplayName("getTransactionHistory - success returns repository data")
    void getTransactionHistory_success() {
        Account a = makeAccount("H2", "owner2", BigDecimal.ZERO, true, AccountType.SAVINGS);
        Transaction t = new Transaction(); t.setStatus("SUCCESS"); t.setAccount(a);
        when(accountRepository.findByAccountNumber("H2")).thenReturn(Optional.of(a));
        when(transactionRepository.findByAccount(a)).thenReturn(List.of(t));

        List<Transaction> history = transactionService.getTransactionHistory("H2", "owner2");
        assertEquals(1, history.size());
        assertSame(t, history.get(0));
    }

    // ---------------- getTransactionById ----------------

    @Test
    @DisplayName("getTransactionById - not found")
    void getTransactionById_notFound() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> transactionService.getTransactionById(999L));
    }

    @Test
    @DisplayName("getTransactionById - found")
    void getTransactionById_found() {
        Transaction t = new Transaction(); t.setId(2L);
        when(transactionRepository.findById(2L)).thenReturn(Optional.of(t));
        Transaction got = transactionService.getTransactionById(2L);
        assertSame(t, got);
    }

    // ---------------- executeRecurringTransfer ----------------

    @Test
    @DisplayName("executeRecurringTransfer - target not found")
    void execRecurring_targetNotFound() {
        Account from = makeAccount("R1", "me", new BigDecimal("1000"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("NO" )).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.executeRecurringTransfer(from, "NO", new BigDecimal("10")));
    }

    @Test
    @DisplayName("executeRecurringTransfer - same account")
    void execRecurring_sameAccount() {
        Account from = makeAccount("R2", "me", new BigDecimal("1000"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("R2")).thenReturn(Optional.of(from));
        assertThrows(IllegalArgumentException.class, () -> transactionService.executeRecurringTransfer(from, "R2", new BigDecimal("10")));
    }

    @Test
    @DisplayName("executeRecurringTransfer - source inactive")
    void execRecurring_sourceInactive() {
        Account from = makeAccount("R3", "me", new BigDecimal("1000"), false, AccountType.SAVINGS);
        Account to = makeAccount("R4", "you", new BigDecimal("0"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("R4")).thenReturn(Optional.of(to));
        assertThrows(IllegalArgumentException.class, () -> transactionService.executeRecurringTransfer(from, "R4", new BigDecimal("10")));
    }

    @Test
    @DisplayName("executeRecurringTransfer - target inactive")
    void execRecurring_targetInactive() {
        Account from = makeAccount("R5", "me", new BigDecimal("1000"), true, AccountType.SAVINGS);
        Account to = makeAccount("R6", "you", new BigDecimal("0"), false, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("R6")).thenReturn(Optional.of(to));
        assertThrows(IllegalArgumentException.class, () -> transactionService.executeRecurringTransfer(from, "R6", new BigDecimal("10")));
    }

    @Test
    @DisplayName("executeRecurringTransfer - insufficient balance")
    void execRecurring_insufficient() {
        Account from = makeAccount("R7", "me", new BigDecimal("5"), true, AccountType.SAVINGS);
        Account to = makeAccount("R8", "you", new BigDecimal("0"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("R8")).thenReturn(Optional.of(to));
        assertThrows(IllegalArgumentException.class, () -> transactionService.executeRecurringTransfer(from, "R8", new BigDecimal("10")));
    }

    @Test
    @DisplayName("executeRecurringTransfer - success")
    void execRecurring_success() {
        Account from = makeAccount("R9", "me", new BigDecimal("500"), true, AccountType.SAVINGS);
        Account to = makeAccount("R10", "you", new BigDecimal("100"), true, AccountType.SAVINGS);
        when(accountRepository.findByAccountNumber("R10")).thenReturn(Optional.of(to));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transactionService.executeRecurringTransfer(from, "R10", new BigDecimal("200"));

        // check balances saved
        verify(accountRepository, atLeastOnce()).save(accountCaptor.capture());
        List<Account> saved = accountCaptor.getAllValues();
        assertTrue(saved.stream().anyMatch(a -> a.getAccountNumber().equals("R9") && a.getBalance().compareTo(new BigDecimal("300"))==0));
        assertTrue(saved.stream().anyMatch(a -> a.getAccountNumber().equals("R10") && a.getBalance().compareTo(new BigDecimal("300"))==0));

        // transactions created
        verify(transactionRepository, times(2)).save(transactionCaptor.capture());
    }

    // ---------------- getTransactionsForAccount ----------------

    @Test
    @DisplayName("getTransactionsForAccount - account not found")
    void getTxForAccount_notFound() {
        when(accountRepository.findByAccountNumber("XX")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> transactionService.getTransactionsForAccount("XX"));
    }

    @Test
    @DisplayName("getTransactionsForAccount - success")
    void getTxForAccount_success() {
        Account a = makeAccount("Y1", "u", BigDecimal.ZERO, true, AccountType.SAVINGS);
        Transaction t = new Transaction(); t.setAccount(a);
        when(accountRepository.findByAccountNumber("Y1")).thenReturn(Optional.of(a));
        when(transactionRepository.findByAccount(a)).thenReturn(List.of(t));

        List<Transaction> res = transactionService.getTransactionsForAccount("Y1");
        assertEquals(1, res.size());
        assertSame(t, res.get(0));
    }
}

