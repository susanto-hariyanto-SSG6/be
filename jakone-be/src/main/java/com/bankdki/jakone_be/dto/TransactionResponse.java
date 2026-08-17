package com.bankdki.jakone_be.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    private String accountNumber;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal newBalance;
    private LocalDateTime timestamp;

    public TransactionResponse(String accountNumber, String transactionType, BigDecimal amount, BigDecimal newBalance, LocalDateTime timestamp) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.amount = amount;
        this.newBalance = newBalance;
        this.timestamp = timestamp;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getTransactionType() { return transactionType; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getNewBalance() { return newBalance; }
    public LocalDateTime getTimestamp() { return timestamp; }
}