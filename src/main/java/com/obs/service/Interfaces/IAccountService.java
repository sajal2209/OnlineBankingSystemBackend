
package com.obs.service.Interfaces;

import com.obs.entity.Account;
import com.obs.payload.request.CreateAccountRequest;
import com.obs.payload.response.AccountDetailsResponse;

import java.math.BigDecimal;
import java.util.List;

public interface IAccountService {

    List<Account> getMyAccounts(String username);

    Account createAccount(CreateAccountRequest request, String username);

    AccountDetailsResponse searchAccount(String accountNumber);

    void toggleAccountActive(String accountNumber);

    BigDecimal deposit(String accountNumber, BigDecimal amount, String bankerUsername);

    Account getByAccountNumber(String accountNumber);

    Account getOwnedAccount(String accountNumber, String username);
}
