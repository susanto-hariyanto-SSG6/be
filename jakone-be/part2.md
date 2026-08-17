
### Part 2 Concept & Architecture

Plaintext

```
HTTP Request (type = "DEPOSIT" | "WITHDRAWAL")
       │
       ▼
AccountController
       │
       ▼
AccountService ──▶ Map<String, TransactionStrategy>
                     ├── "DEPOSIT"    ──▶ DepositStrategy Bean
                     └── "WITHDRAWAL" ──▶ WithdrawalStrategy Bean

```

### Step 1: Create Request & Response DTOs

#### A. Transaction Request DTO

Create `src/main/java/com/bankdki/jakone_be/dto/TransactionRequest.java`:

Java

```
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

```

#### B. Transaction Response DTO

Create `src/main/java/com/bankdki/jakone_be/dto/TransactionResponse.java`:

Java

```
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

```

### Step 2: Define the `TransactionStrategy` Interface

Create `src/main/java/com/bankdki/jakone_be/strategy/TransactionStrategy.java`:

Java

```
package com.bankdki.jakone_be.strategy;

import com.bankdki.jakone_be.entity.Account;
import java.math.BigDecimal;

public interface TransactionStrategy {
    void execute(Account account, BigDecimal amount);
}

```

### Step 3: Implement Strategy Spring Beans

#### A. Deposit Strategy (`@Component("DEPOSIT")`)

Create `src/main/java/com/bankdki/jakone_be/strategy/DepositStrategy.java`:

Java

```
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

```

#### B. Withdrawal Strategy (`@Component("WITHDRAWAL")`)

Create `src/main/java/com/bankdki/jakone_be/strategy/WithdrawalStrategy.java`:

Java

```
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

```

### Step 4: Inject Strategy Map into `AccountService`

Update `src/main/java/com/bankdki/jakone_be/service/AccountService.java`:

Java

```
package com.bankdki.jakone_be.service;

import com.bankdki.jakone_be.dto.RegisterRequest;
import com.bankdki.jakone_be.dto.TransactionRequest;
import com.bankdki.jakone_be.dto.TransactionResponse;
import com.bankdki.jakone_be.entity.Account;
import com.bankdki.jakone_be.repository.AccountRepository;
import com.bankdki.jakone_be.strategy.TransactionStrategy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    // Spring automatically injects all Beans implementing TransactionStrategy into this Map,
    // keyed by their component name ("DEPOSIT", "WITHDRAWAL")
    private final Map<String, TransactionStrategy> transactionStrategies;

    public AccountService(AccountRepository accountRepository, 
                          Map<String, TransactionStrategy> transactionStrategies) {
        this.accountRepository = accountRepository;
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

    public TransactionResponse processTransaction(String accountNumber, TransactionRequest request) {
        Account account = getAccountByNumber(accountNumber);

        // Fetch strategy bean dynamically using the request type ("DEPOSIT" or "WITHDRAWAL")
        TransactionStrategy strategy = transactionStrategies.get(request.getType().toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Invalid transaction type: " + request.getType());
        }

        // Execute domain strategy logic
        strategy.execute(account, request.getAmount());

        // Save updated balance
        accountRepository.save(account);

        return new TransactionResponse(
            account.getAccountNumber(),
            request.getType().toUpperCase(),
            request.getAmount(),
            account.getBalance(),
            LocalDateTime.now()
        );
    }
}

```

### Step 5: Expose Transaction Endpoint in `AccountController`

Update `src/main/java/com/bankdki/jakone_be/controller/AccountController.java`:

Java

```
package com.bankdki.jakone_be.controller;

import com.bankdki.jakone_be.dto.RegisterRequest;
import com.bankdki.jakone_be.dto.TransactionRequest;
import com.bankdki.jakone_be.dto.TransactionResponse;
import com.bankdki.jakone_be.entity.Account;
import com.bankdki.jakone_be.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<Account> register(@RequestBody RegisterRequest request) {
        Account createdAccount = accountService.registerAccount(request);
        return new ResponseEntity<>(createdAccount, HttpStatus.CREATED);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<Account> getAccount(@PathVariable String accountNumber) {
        Account account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(account);
    }

    // Core Use Cases 2 & 3: Add Funds & Withdraw
    @PostMapping("/{accountNumber}/transact")
    public ResponseEntity<TransactionResponse> transact(
            @PathVariable String accountNumber,
            @RequestBody TransactionRequest request) {
        TransactionResponse response = accountService.processTransaction(accountNumber, request);
        return ResponseEntity.ok(response);
    }
}

```

### 🛑 Instructor Checkpoint #3 (Test Strategy Routing)

Rebuild and spin up the Docker container or run locally to test the new endpoint.

#### Test 1: Add Funds / Deposit

PowerShell

```
$body = @'
{
  "type": "DEPOSIT",
  "channel": "TRANSFER",
  "amount": 250000.00
}
'@

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/DKI-1000001/transact" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

```

#### Test 2: Withdraw Funds

PowerShell

```
$body = @'
{
  "type": "WITHDRAWAL",
  "channel": "CASH",
  "amount": 100000.00
}
'@

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/DKI-1000001/transact" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```
