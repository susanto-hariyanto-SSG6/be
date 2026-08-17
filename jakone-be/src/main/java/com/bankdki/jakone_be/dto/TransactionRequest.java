package com.bankdki.jakone_be.dto;

import java.math.BigDecimal;

public class TransactionRequest {
    private String type;    // "DEPOSIT" or "WITHDRAWAL"
    private String channel; // "CASH", "TRANSFER", "QRIS"
    private BigDecimal amount;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}