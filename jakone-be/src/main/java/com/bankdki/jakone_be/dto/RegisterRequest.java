package com.bankdki.jakone_be.dto;

import java.math.BigDecimal;

public class RegisterRequest {
    private String customerNik;
    private String customerName;
    private BigDecimal initialBalance;

    public String getCustomerNik() { return customerNik; }
    public void setCustomerNik(String customerNik) { this.customerNik = customerNik; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}