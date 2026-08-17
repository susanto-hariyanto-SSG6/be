package com.bankdki.jakone_be.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class TransactionRequest {
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(?i)(DEPOSIT|WITHDRAWAL)$", message = "Transaction type must be DEPOSIT or WITHDRAWAL")
    private String type;

    @NotBlank(message = "Channel is required")
    @Pattern(regexp = "^(?i)(CASH|TRANSFER|QRIS)$", message = "Channel must be CASH, TRANSFER, or QRIS")
    private String channel;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000.00", message = "Transaction amount must be at least 10,000.00")
    private BigDecimal amount;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}