package com.bankdki.jakone_be.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    @NotBlank(message = "Customer NIK is required")
    @Size(min = 16, max = 16, message = "Customer NIK must be exactly 16 digits")
    private String customerNik;

    @NotBlank(message = "Customer Name is required")
    @Size(min = 3, max = 100, message = "Customer Name must be between 3 and 100 characters")
    private String customerName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "50000.00", message = "Initial balance must be at least 50,000.00")
    private BigDecimal initialBalance;

    public String getCustomerNik() { return customerNik; }
    public void setCustomerNik(String customerNik) { this.customerNik = customerNik; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}