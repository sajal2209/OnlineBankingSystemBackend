package com.obs.payload.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class TransferRequest {
    @NotBlank
    @jakarta.validation.constraints.Size(min = 16, max = 16, message = "From account number must be 16 digits")
    @jakarta.validation.constraints.Pattern(regexp = "\\d+", message = "From account number must contain only digits")
    private String fromAccountNumber;

    @NotBlank
    @jakarta.validation.constraints.Size(min = 16, max = 16, message = "To account number must be 16 digits")
    @jakarta.validation.constraints.Pattern(regexp = "\\d+", message = "To account number must contain only digits")
    private String toAccountNumber;

    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
