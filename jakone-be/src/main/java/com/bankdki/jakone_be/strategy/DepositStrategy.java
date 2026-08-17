package com.bankdki.jakone_be.strategy;

import com.bankdki.jakone_be.entity.Account;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("DEPOSIT")
public class DepositStrategy implements TransactionStrategy {

    @Override
    public void execute(Account account, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }
        account.setBalance(account.getBalance().add(amount));
    }
}