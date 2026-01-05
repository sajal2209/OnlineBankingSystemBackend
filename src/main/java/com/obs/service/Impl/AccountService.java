
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

import com.obs.repository.UserRepository;
import com.obs.service.Interfaces.IAccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService implements IAccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    public AccountService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              UserService userService,UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getMyAccounts(String username) {
        User user = userService.getByUsername(username);
        return accountRepository.findByUser(user);
    }

    @Override
    @Transactional
    public Account createAccount(CreateAccountRequest request, String username)
    {
        User user1 = userRepository.findByUsername(username).orElseThrow(()->new RuntimeException("user not found"));
        String requestPan = request.getPanCardNumber();
        if(user1.getPanNumber()==null)
        {
            if (userRepository.existsBypanCardNumber(requestPan))
            {
                throw  new IllegalArgumentException("PAN already linked to another customer");
            }
            user1.setPanNumber(requestPan);
            userRepository.save(user1);
        }
        else {
            if(!user1.getPanNumber().equals(requestPan)){
                throw new IllegalArgumentException("customer can use only one pan number for all accounts");
            }
        }
        User user = userService.getByUsername(username);

        // Validate CURRENT account fields
        if (request.getAccountType() == AccountType.CURRENT) {
            if (isBlank(request.getBusinessName()) || isBlank(request.getBusinessAddress())) {
                throw new IllegalArgumentException("Business Name and Address are required for Current Account.");
            }
        }

        Account account = new Account();
        account.setAccountType(request.getAccountType());
        account.setBalance(BigDecimal.ZERO);
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
//        account.setPanCardNumber(request.getPanCardNumber());

        if (request.getAccountType() == AccountType.CURRENT) {
            account.setBusinessName(request.getBusinessName());
            account.setBusinessAddress(request.getBusinessAddress());
        }

        return accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDetailsResponse searchAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        return new AccountDetailsResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.isActive(),
                account.getUser().getUsername(),
                account.getUser().getId(),
                account.getUser().getEmail()
        );
    }

    @Override
    @Transactional
    public void toggleAccountActive(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setActive(!account.isActive());
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public BigDecimal deposit(String accountNumber, BigDecimal amount, String bankerUsername) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.isActive()) {
            throw new IllegalArgumentException("Cannot deposit to frozen/inactive account");
        }

        // Update balance
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        // Record Transaction (CREDIT)
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount); // positive for credit
        transaction.setType("CREDIT");
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setDescription("Cash Deposit by Banker" + (bankerUsername != null ? " [" + bankerUsername + "]" : ""));
        transaction.setStatus("SUCCESS");
        transactionRepository.save(transaction);

        return account.getBalance();
    }

    // --- Helpers ---

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Generates a 16-digit account number starting with 1000 (e.g., 1000XXXXXXXXXXXX).
     * Uses SecureRandom to avoid predictability.
     */
    private String generateAccountNumber() {
        SecureRandom rand = new SecureRandom();
        StringBuilder sb = new StringBuilder("1000");
        for (int i = 0; i < 12; i++) {
            sb.append(rand.nextInt(10));
        }
        return sb.toString();
    }


    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Account getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new com.obs.exception.ResourceNotFoundException("Account not found"));
    }



    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Account getOwnedAccount(String accountNumber, String username) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new com.obs.exception.ResourceNotFoundException("Account not found"));

        if (!account.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("Unauthorized access to account");
        }
        return account;
    }


}
