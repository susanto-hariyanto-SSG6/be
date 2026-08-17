package com.bankdki.jakone_be.service;

import com.bankdki.jakone_be.dto.RegisterRequest;
import com.bankdki.jakone_be.entity.Account;
import com.bankdki.jakone_be.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account registerAccount(RegisterRequest request) {
        String generatedAccountNumber = "DKI-" + (1000000 + new Random().nextInt(9000000));

        Account account = new Account(
            generatedAccountNumber,
            request.getCustomerNik(),
            request.getCustomerName(),
            request.getInitialBalance()
        );

        return accountRepository.save(account);
    }

    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));
    }
}