
package com.obs.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import com.obs.entity.Account;
import com.obs.payload.request.CreateAccountRequest;
import com.obs.payload.response.AccountDetailsResponse;
import com.obs.payload.response.MessageResponse;

import com.obs.service.Interfaces.IAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private IAccountService accountService;


    @GetMapping("/my-accounts")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('BANKER') or hasRole('ADMIN')")
    public List<Account> getMyAccounts(Principal principal) {
        return accountService.getMyAccounts(principal.getName());
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> createAccount(@RequestBody CreateAccountRequest request, Principal principal) {
        accountService.createAccount(request, principal.getName());
        return ResponseEntity.ok(new MessageResponse("Account created successfully!"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> searchAccount(@RequestParam String accountNumber) {
        AccountDetailsResponse response = accountService.searchAccount(accountNumber);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountNumber}/toggle-active")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> toggleAccountActive(@PathVariable String accountNumber) {
        accountService.toggleAccountActive(accountNumber);
        return ResponseEntity.ok(new MessageResponse("Account status updated successfully"));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasRole('BANKER')")
    public ResponseEntity<?> deposit(@RequestBody com.obs.payload.request.DepositRequest request,
                                     Principal principal) {
        BigDecimal newBalance = accountService.deposit(request.getAccountNumber(), request.getAmount(), principal.getName());
        return ResponseEntity.ok(new MessageResponse("Signal deposit successful. New balance: " + newBalance));
    }
}
