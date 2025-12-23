package com.obs.service.Interfaces;

import com.obs.entity.Account;
import com.obs.entity.Transaction;

import java.util.List;

public interface IPdfGenerationService {


    byte[] generateTransactionInvoice(Transaction transaction);

    byte[] generateAccountStatement(Account account, List<Transaction> transactions);

}
