package com.bankdki.jakone_be.strategy;

import com.bankdki.jakone_be.entity.Account;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component("WITHDRAWAL")
public class WithdrawalStrategy implements TransactionStrategy {

    @Override
    public void execute(Account account, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
    }
}