
# **Spring Validation & Global Exception Handling**

This part ensures input payloads conform to business constraints before reaching the service layer and returns structured RFC 7807-style error responses instead of standard HTTP 500 server crashes.

### Step 1: Add Validation Dependency (`pom.xml`)

Add `spring-boot-starter-validation` to your `pom.xml`:

XML

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

```

### Step 2: Add Validation Constraints to DTOs

#### A. `RegisterRequest.java`

Update `src/main/java/com/bankdki/jakone_be/dto/RegisterRequest.java`:

Java

```
package com.bankdki.jakone_be.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class RegisterRequest {

    @NotBlank(message = "Customer NIK is required")
    @Size(min = 16, max = 16, message = "Customer NIK must be exactly 16 digits")
    private String customerNik;

    @NotBlank(message = "Customer Name is required")
    @Size(min = 3, max = 100, message = "Customer Name must be between 3 and 100 characters")
    private String customerName;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "50000.00", message = "Initial balance must be at least 50,000.00")
    private BigDecimal initialBalance;

    public String getCustomerNik() { return customerNik; }
    public void setCustomerNik(String customerNik) { this.customerNik = customerNik; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}

```

#### B. `TransactionRequest.java`

Update `src/main/java/com/bankdki/jakone_be/dto/TransactionRequest.java`:

Java

```
package com.bankdki.jakone_be.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

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

```

### Step 3: Enable `@Valid` in `AccountController`

Update `src/main/java/com/bankdki/jakone_be/controller/AccountController.java` to apply `@Valid` on request bodies:

Java

```
package com.bankdki.jakone_be.controller;

import com.bankdki.jakone_be.dto.RegisterRequest;
import com.bankdki.jakone_be.dto.TransactionRequest;
import com.bankdki.jakone_be.dto.TransactionResponse;
import com.bankdki.jakone_be.entity.Account;
import com.bankdki.jakone_be.entity.Mutation;
import com.bankdki.jakone_be.service.AccountService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Account> register(@Valid @RequestBody RegisterRequest request) {
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
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = accountService.processTransaction(accountNumber, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}/mutations")
    public ResponseEntity<List<Mutation>> getMutations(@PathVariable String accountNumber) {
        List<Mutation> mutations = accountService.getAccountMutations(accountNumber);
        return ResponseEntity.ok(mutations);
    }
}

```

### Step 4: Create `GlobalExceptionHandler`

Create `src/main/java/com/bankdki/jakone_be/exception/GlobalExceptionHandler.java` to handle validation errors and business exceptions:

Java

```
package com.bankdki.jakone_be.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handle Payload Validation Errors (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");
        body.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 2. Handle Business & Domain Logic Exceptions
    @ExceptionHandler({RuntimeException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBusinessException(RuntimeException ex) {
        log.warn("Business logic exception: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 3. Fallback for Uncaught Server Errors
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled System Exception: ", ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact support.");

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

```

### 🧪 Verification Commands (PowerShell)

#### Test 1: Validation Failure Test (Invalid NIK & Low Balance)

PowerShell

```
$invalidBody = @'
{
  "customerNik": "123",
  "customerName": "A",
  "initialBalance": 1000.00
}
'@

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts" `
  -Method Post `
  -ContentType "application/json" `
  -Body $invalidBody `
  -HttpVersion 1.1

```

**Expected HTTP 400 Output:**

JSON

```
{
  "timestamp": "2026-08-17T08:15:22.102",
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "customerNik": "Customer NIK must be exactly 16 digits",
    "customerName": "Customer Name must be between 3 and 100 characters",
    "initialBalance": "Initial balance must be at least 50,000.00"
  }
}

```

#### Test 2: Invalid Transaction Type

PowerShell

```
$txInvalid = @'
{
  "type": "LOAN",
  "channel": "ATM",
  "amount": 50000.00
}
'@

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/DKI-1000001/transact" `
  -Method Post `
  -ContentType "application/json" `
  -Body $txInvalid `
  -HttpVersion 1.1
```
