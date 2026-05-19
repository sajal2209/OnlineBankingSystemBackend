
package com.obs.payload.request;

import java.math.BigDecimal;

public class DepositRequest {
    @jakarta.validation.constraints.NotBlank(message = "Account Number is required")
    @jakarta.validation.constraints.Size(min = 16, max = 16, message = "Account number must be 16 digits")
    @jakarta.validation.constraints.Pattern(regexp = "\\d+", message = "Account number must contain only digits")
    private String accountNumber;
    private BigDecimal amount;

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
