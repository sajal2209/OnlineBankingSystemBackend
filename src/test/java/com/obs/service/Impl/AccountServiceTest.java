package com.obs.service.Impl;

import com.obs.entity.Account;
import com.obs.entity.AccountType;
import com.obs.entity.Transaction;
import com.obs.entity.User;
import com.obs.exception.ResourceNotFoundException;
import com.obs.payload.request.CreateAccountRequest;
import com.obs.payload.response.AccountDetailsResponse;
import com.obs.repository.AccountRepository;
import com.obs.repository.TransactionRepository;

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
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    AccountRepository accountRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    UserService userService;

    @InjectMocks
    AccountService accountService;

    @Captor
    ArgumentCaptor<Account> accountCaptor;

    @Captor
    ArgumentCaptor<Transaction> transactionCaptor;

    @Test
    @DisplayName("getMyAccounts returns accounts for user")
    void getMyAccounts_returnsList() {
        User u = new User();
        u.setUsername("alice");
        when(userService.getByUsername("alice")).thenReturn(u);

        Account a1 = new Account(); a1.setAccountNumber("A1");
        Account a2 = new Account(); a2.setAccountNumber("A2");
        when(accountRepository.findByUser(u)).thenReturn(List.of(a1, a2));

        List<Account> out = accountService.getMyAccounts("alice");
        assertEquals(2, out.size());
        assertEquals("A1", out.iterator().next().getAccountNumber());
    }

    @Test
    @DisplayName("createAccount - saving account sets pan and generates account number")
    void createAccount_saving() {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.SAVINGS);
        req.setPanCardNumber("PAN123");

        User u = new User(); u.setUsername("bob");
        when(userService.getByUsername("bob")).thenReturn(u);
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Account saved = accountService.createAccount(req, "bob");

        assertEquals(AccountType.SAVINGS, saved.getAccountType());
        assertEquals(BigDecimal.ZERO, saved.getBalance());
        assertSame(u, saved.getUser());
        assertEquals("PAN123", saved.getPanCardNumber());

        assertNotNull(saved.getAccountNumber());
        assertTrue(Pattern.matches("^1000\\d{12}$", saved.getAccountNumber()));
    }

    @Test
    @DisplayName("createAccount - current account requires business fields")
    void createAccount_currentRequiresBusiness() {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.CURRENT);
        req.setPanCardNumber("PANX");
        req.setBusinessName(""); // blank
        req.setBusinessAddress(null);

        User u = new User(); u.setUsername("c");
        when(userService.getByUsername("c")).thenReturn(u);

        assertThrows(IllegalArgumentException.class, () -> accountService.createAccount(req, "c"));
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("createAccount - current account success")
    void createAccount_currentSuccess() {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(AccountType.CURRENT);
        req.setPanCardNumber("PANY");
        req.setBusinessName("B Ltd");
        req.setBusinessAddress("Addr");

        User u = new User(); u.setUsername("owner");
        when(userService.getByUsername("owner")).thenReturn(u);
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Account out = accountService.createAccount(req, "owner");
        assertEquals(AccountType.CURRENT, out.getAccountType());
        assertEquals("B Ltd", out.getBusinessName());
        assertEquals("Addr", out.getBusinessAddress());
        assertEquals("PANY", out.getPanCardNumber());
    }

    @Test
    @DisplayName("searchAccount returns details when found")
    void searchAccount_found() {
        User u = new User(); u.setUsername("who"); u.setId(99L); u.setEmail("who@ex");
        Account a = new Account(); a.setId(7L); a.setAccountNumber("ACC7"); a.setBalance(new BigDecimal("123.45")); a.setActive(true); a.setUser(u);
        when(accountRepository.findByAccountNumber("ACC7")).thenReturn(Optional.of(a));

        AccountDetailsResponse r = accountService.searchAccount("ACC7");
        assertEquals(7L, r.getId());
        assertEquals("ACC7", r.getAccountNumber());
        assertEquals(new BigDecimal("123.45"), r.getBalance());
        assertTrue(r.isActive());
        assertEquals("who", r.getUsername());
        assertEquals(99L, r.getUserId());
        assertEquals("who@ex", r.getEmail());
    }

    @Test
    @DisplayName("searchAccount throws when not found")
    void searchAccount_notFound() {
        when(accountRepository.findByAccountNumber("X" )).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> accountService.searchAccount("X"));
    }

    @Test
    @DisplayName("toggleAccountActive flips active and saves")
    void toggleAccountActive_flips() {
        Account a = new Account(); a.setAccountNumber("T1"); a.setActive(true);
        when(accountRepository.findByAccountNumber("T1")).thenReturn(Optional.of(a));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        accountService.toggleAccountActive("T1");

        verify(accountRepository).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertFalse(saved.isActive());
    }

    @Test
    @DisplayName("toggleAccountActive throws when not found")
    void toggleAccountActive_notFound() {
        when(accountRepository.findByAccountNumber("NO" )).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> accountService.toggleAccountActive("NO"));
    }

    @Test
    @DisplayName("deposit increases balance, saves transaction with banker name")
    void deposit_success_withBanker() {
        Account a = new Account(); a.setAccountNumber("D1"); a.setBalance(new BigDecimal("100")); a.setActive(true);
        when(accountRepository.findByAccountNumber("D1")).thenReturn(Optional.of(a));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal res = accountService.deposit("D1", new BigDecimal("50"), "banker1");
        assertEquals(new BigDecimal("150"), res);

        verify(accountRepository).save(accountCaptor.capture());
        Account saved = accountCaptor.getValue();
        assertEquals(0, saved.getBalance().compareTo(new BigDecimal("150")));

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction tx = transactionCaptor.getValue();
        assertEquals("CREDIT", tx.getType());
        assertTrue(tx.getDescription().contains("banker1"));
        assertEquals("SUCCESS", tx.getStatus());
    }

    @Test
    @DisplayName("deposit with null bankerName doesn't add brackets")
    void deposit_success_noBankerName() {
        Account a = new Account(); a.setAccountNumber("D2"); a.setBalance(new BigDecimal("10")); a.setActive(true);
        when(accountRepository.findByAccountNumber("D2")).thenReturn(Optional.of(a));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BigDecimal res = accountService.deposit("D2", new BigDecimal("5"), null);
        assertEquals(new BigDecimal("15"), res);

        verify(transactionRepository).save(transactionCaptor.capture());
        Transaction tx = transactionCaptor.getValue();
        assertEquals("Cash Deposit by Banker", tx.getDescription());
    }

    @Test
    @DisplayName("deposit throws when account inactive")
    void deposit_inactiveThrows() {
        Account a = new Account(); a.setAccountNumber("D3"); a.setBalance(new BigDecimal("0")); a.setActive(false);
        when(accountRepository.findByAccountNumber("D3")).thenReturn(Optional.of(a));

        assertThrows(IllegalArgumentException.class, () -> accountService.deposit("D3", new BigDecimal("10"), "b"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("getByAccountNumber returns account or throws")
    void getByAccountNumber_paths() {
        Account a = new Account(); a.setAccountNumber("G1");
        when(accountRepository.findByAccountNumber("G1")).thenReturn(Optional.of(a));
        assertSame(a, accountService.getByAccountNumber("G1"));
        when(accountRepository.findByAccountNumber("NX")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> accountService.getByAccountNumber("NX"));
    }

    @Test
    @DisplayName("getOwnedAccount returns account when owner matches and throws when not")
    void getOwnedAccount_paths() {
        User u = new User(); u.setUsername("owner");
        Account a = new Account(); a.setAccountNumber("OWN1"); a.setUser(u);
        when(accountRepository.findByAccountNumber("OWN1")).thenReturn(Optional.of(a));
        assertSame(a, accountService.getOwnedAccount("OWN1", "owner"));

        // unauthorized
        assertThrows(IllegalArgumentException.class, () -> accountService.getOwnedAccount("OWN1", "intruder"));
    }
}
