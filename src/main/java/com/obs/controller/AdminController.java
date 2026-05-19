
package com.obs.controller;

import com.obs.entity.Transaction;
import com.obs.entity.User;
import com.obs.payload.request.SignupRequest;
import com.obs.payload.response.MessageResponse;

import com.obs.service.Interfaces.ITransactionService;
import com.obs.service.Interfaces.IUserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private  IUserService userService;

    @Autowired
    private ITransactionService transactionService;

    @PostMapping("/create-banker")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createBanker(@Valid @RequestBody SignupRequest signUpRequest) {
        userService.createBanker(signUpRequest);
        return ResponseEntity.ok(new MessageResponse("Banker registered successfully!"));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllNonAdminUsers());
    }

    @PutMapping("/users/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserActive(@PathVariable Long id) {
        userService.toggleUserActive(id);
        return ResponseEntity.ok(new MessageResponse("User activation status updated successfully!"));
    }

    @GetMapping("/pending-transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Transaction> getPendingTransactions() {
        return transactionService.getPendingTransactions();
    }

    @PutMapping("/transactions/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveTransaction(@PathVariable Long id) {
        transactionService.approveTransaction(id);
        return ResponseEntity.ok(new MessageResponse("Transaction approved successfully by Admin"));
    }

    @PutMapping("/transactions/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectTransaction(@PathVariable Long id) {
        transactionService.rejectTransaction(id);
        return ResponseEntity.ok(new MessageResponse("Transaction rejected successfully by Admin"));
    }

}

