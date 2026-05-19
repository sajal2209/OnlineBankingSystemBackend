
package com.obs.controller;

import com.obs.entity.Account;
import com.obs.entity.Transaction;
import com.obs.payload.response.MessageResponse;
import com.obs.service.Interfaces.IAccountService;
import com.obs.service.Interfaces.IPdfGenerationService;
import com.obs.service.Interfaces.ITransactionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banker")
public class BankerController {

    private final ITransactionService transactionService;
    private final IAccountService accountService;
    private final IPdfGenerationService pdfGenerationService;

    public BankerController(ITransactionService transactionService,
                            IAccountService accountService,
                            IPdfGenerationService pdfGenerationService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.pdfGenerationService = pdfGenerationService;
    }

    @GetMapping("/pending-transactions")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public List<Transaction> getPendingTransactions() {
        return transactionService.getPendingTransactions();
    }

    @PutMapping("/transactions/{id}/approve")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<?> approveTransaction(@PathVariable Long id) {
        transactionService.approveTransaction(id);
        return ResponseEntity.ok(new MessageResponse("Transaction approved successfully"));
    }

    @PutMapping("/transactions/{id}/reject")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<?> rejectTransaction(@PathVariable Long id) {
        transactionService.rejectTransaction(id);
        return ResponseEntity.ok(new MessageResponse("Transaction rejected successfully"));
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public List<Transaction> getAccountTransactions(@PathVariable String accountNumber) {
        return transactionService.getTransactionsForAccount(accountNumber);
    }

    @GetMapping("/accounts/{accountNumber}/statement")
    @PreAuthorize("hasRole('BANKER') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadAccountStatement(@PathVariable String accountNumber) {
        Account account = accountService.getByAccountNumber(accountNumber);
        List<Transaction> transactions = transactionService.getTransactionsForAccount(accountNumber);

        byte[] pdfBytes = pdfGenerationService.generateAccountStatement(account, transactions);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=statement_" + accountNumber + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
