package com.obs.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class RecurringPaymentRequest {
    @NotBlank
    @jakarta.validation.constraints.Size(min = 16, max = 16, message = "Account number must be 16 digits")
    @jakarta.validation.constraints.Pattern(regexp = "\\d+", message = "Account number must contain only digits")
    private String fromAccountNumber;

    @NotNull
    private BigDecimal amount;

    @NotBlank
    @jakarta.validation.constraints.Size(min = 16, max = 16, message = "Target account number must be 16 digits")
    @jakarta.validation.constraints.Pattern(regexp = "\\d+", message = "Target account number must contain only digits")
    private String targetAccountNumber;

    @NotBlank
    private String frequency; // DAILY, WEEKLY, MONTHLY

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTargetAccountNumber() {
        return targetAccountNumber;
    }

    public void setTargetAccountNumber(String targetAccountNumber) {
        this.targetAccountNumber = targetAccountNumber;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
