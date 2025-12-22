package com.obs.service.Interfaces;

import com.obs.entity.Account;
import com.obs.entity.Transaction;
import com.obs.payload.request.TransferRequest;

import java.math.BigDecimal;
import java.util.List;

public interface ITransactionService {


    String transferFunds(TransferRequest transferRequest, String username);


    void approveTransaction(Long transactionId);


    void rejectTransaction(Long transactionId);


    List<Transaction> getPendingTransactions();


    List<Transaction> getTransactionHistory(String accountNumber, String username);


    Transaction getTransactionById(Long id);

    void executeRecurringTransfer(Account fromAccount, String targetAccountNumber, BigDecimal amount);


    List<Transaction> getTransactionsForAccount(String accountNumber);

}
