
# **Atomic Transactions, Pessimistic Locking & Mutation Ledger (`dki_mutations`)** 

Focuses on ensuring strict data consistency under high concurrent access and keeping an immutable audit trail of every balance change.

### Step 1: Create the `Mutation` Entity

Create `src/main/java/com/bankdki/jakone_be/entity/Mutation.java`:

Java

```
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

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Mutation() {}

    public Mutation(String accountNumber, String transactionType, String channel, BigDecimal amount, BigDecimal balanceAfter) {
        this.accountNumber = accountNumber;
        this.transactionType = transactionType;
        this.channel = channel;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public String getTransactionType() { return transactionType; }
    public String getChannel() { return channel; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

```

### Step 2: Add Pessimistic Locking to `AccountRepository`

Update `src/main/java/com/bankdki/jakone_be/repository/AccountRepository.java` to issue `SELECT ... FOR UPDATE` SQL locks, preventing concurrent thread race conditions during transaction updates.

Java

```
package com.bankdki.jakone_be.repository;

import com.bankdki.jakone_be.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    // Pessimistic Write Lock acquires a row-level lock in PostgreSQL
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}

```

### Step 3: Create `MutationRepository`

Create `src/main/java/com/bankdki/jakone_be/repository/MutationRepository.java`:

Java

```
package com.bankdki.jakone_be.repository;

import com.bankdki.jakone_be.entity.Mutation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MutationRepository extends JpaRepository<Mutation, Long> {
    List<Mutation> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
}

```

### Step 4: Make `AccountService.processTransaction` Atomic with `@Transactional`

Update `src/main/java/com/bankdki/jakone_be/service/AccountService.java` to use the locked fetch and persist the mutation entry inside a single atomic transaction boundary.

Java

```
package com.bankdki.jakone_be.service;

import com.bankdki.jakone_be.dto.RegisterRequest;
import com.bankdki.jakone_be.dto.TransactionRequest;
import com.bankdki.jakone_be.dto.TransactionResponse;
import com.bankdki.jakone_be.entity.Account;
import com.bankdki.jakone_be.entity.Mutation;
import com.bankdki.jakone_be.repository.AccountRepository;
import com.bankdki.jakone_be.repository.MutationRepository;
import com.bankdki.jakone_be.strategy.TransactionStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        // Fetch account with PESSIMISTIC_WRITE lock
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountNumber));

        TransactionStrategy strategy = transactionStrategies.get(request.getType().toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Invalid transaction type: " + request.getType());
        }

        strategy.execute(account, request.getAmount());
        accountRepository.save(account);

        // Record immutable ledger entry
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

```

### Step 5: Expose Mutation Ledger in `AccountController`

Update `src/main/java/com/bankdki/jakone_be/controller/AccountController.java`:

Java

```
package com.bankdki.jakone_be.controller;

import com.bankdki.jakone_be.dto.RegisterRequest;
import com.bankdki.jakone_be.dto.TransactionRequest;
import com.bankdki.jakone_be.dto.TransactionResponse;
import com.bankdki.jakone_be.entity.Account;
import com.bankdki.jakone_be.entity.Mutation;
import com.bankdki.jakone_be.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/{accountNumber}/transact")
    public ResponseEntity<TransactionResponse> transact(
            @PathVariable String accountNumber,
            @RequestBody TransactionRequest request) {
        TransactionResponse response = accountService.processTransaction(accountNumber, request);
        return ResponseEntity.ok(response);
    }

    // Mutation History Ledger
    @GetMapping("/{accountNumber}/mutations")
    public ResponseEntity<List<Mutation>> getMutations(@PathVariable String accountNumber) {
        List<Mutation> mutations = accountService.getAccountMutations(accountNumber);
        return ResponseEntity.ok(mutations);
    }
}

```

### 🧪 Part 3 Verification Commands (PowerShell)

#### 1. Perform Deposit Transaction

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
  -HttpVersion 1.1

```

#### 2. Query Mutation Ledger History

PowerShell

```
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/DKI-1000001/mutations" -Me
```

### Logging Tips

Add this to `application.properties` for detail log during exception / error

```
logging.level.org.hibernate.SQL=DEBUG 
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE 
logging.level.org.springframework.transaction=TRACE 
logging.level.org.springframework.orm.jpa=DEBUG
