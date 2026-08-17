package com.bankdki.jakone_be.service;

import com.bankdki.jakone_be.dto.RegisterRequest;
import com.bankdki.jakone_be.dto.TransactionRequest;
import com.bankdki.jakone_be.dto.TransactionResponse;
import com.bankdki.jakone_be.entity.Account;
import com.bankdki.jakone_be.entity.Mutation;
import com.bankdki.jakone_be.repository.AccountRepository;
import com.bankdki.jakone_be.repository.MutationRepository;
import com.bankdki.jakone_be.strategy.TransactionStrategy;

import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final MutationRepository mutationRepository;
    private final Map<String, TransactionStrategy> transactionStrategies;

    public AccountService(AccountRepository accountRepository,
                          MutationRepository mutationRepository,
                          Map<String, TransactionStrategy> transactionStrategies) {
        this.accountRepository = accountRepository;
        this.mutationRepository = mutationRepository;
        this.transactionStrategies = transactionStrategies;
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

    @Transactional
    public TransactionResponse processTransaction(String accountNumber, TransactionRequest request) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        TransactionStrategy strategy = transactionStrategies.get(request.getType().toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Invalid transaction type: " + request.getType());
        }

        strategy.execute(account, request.getAmount());
        accountRepository.save(account);

        Mutation mutation = new Mutation(
            account.getAccountNumber(),
            request.getType().toUpperCase(),
            request.getChannel(),
            request.getAmount(),
            account.getBalance()
        );
        mutationRepository.save(mutation);

        return new TransactionResponse(
            account.getAccountNumber(),
            request.getType().toUpperCase(),
            request.getAmount(),
            account.getBalance(),
            LocalDateTime.now()
        );
    }

    public List<Mutation> getAccountMutations(String accountNumber) {
        return mutationRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }
}