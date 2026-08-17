package com.bankdki.jakone_be.strategy;

import com.bankdki.jakone_be.entity.Account;
import java.math.BigDecimal;

public interface TransactionStrategy {
    void execute(Account account, BigDecimal amount);
}