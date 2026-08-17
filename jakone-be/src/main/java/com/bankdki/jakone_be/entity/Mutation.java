package com.bankdki.jakone_be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dki_mutations")
public class Mutation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "resulting_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal resultingBalance;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Mutation() {}

    public Mutation(String accountNumber, String transactionType, String channel, BigDecimal amount, BigDecimal resultingBalance) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.channel = channel;
        this.amount = amount;
        this.resultingBalance = resultingBalance;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getTransactionType() { return transactionType; }
    public String getChannel() { return channel; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getResultingBalance() { return resultingBalance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}