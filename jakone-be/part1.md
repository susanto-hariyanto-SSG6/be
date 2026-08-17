
  ## 🎯 Part 1 Learning Objectives

1.  Connect Spring Boot to a containerized PostgreSQL database using **Spring Data JPA**.
    
2.  Map a domain model (`Account`) to an existing database table (`dki_accounts`).
    
3.  Build a REST Controller exposing account creation (`POST`) and profile lookup (`GET`).
    
4.  Execute and verify live endpoints using Docker container environments and API tools.

### Step 1: Generate the Spring Boot Project (10 mins)

1. Open **VS Code**.

2. Press **`Ctrl+Shift+P`** (or `Cmd+Shift+P` on Mac) and type:

**`Spring Initializr: Create a Maven Project`**

3. Select the following settings when prompted:

-  **Language:**  `Java`

-  **Spring Boot Version:**  `4.1.0`

-  **Group ID:**  `com.bankdki`

-  **Artifact ID:**  `jakone-be`

-  **Packaging:**  `Jar`

-  **Java Version:**  `21`

-  **Dependencies:** Search and select the following 3 dependencies:

-  **Spring Web**

-  **Spring Data JPA**

-  **PostgreSQL Driver**

4. Choose a folder on your machine and click **Generate into this folder**, then open the project workspace in VS Code.

  ### Configure Database Connection (10 mins)
  

Open `src/main/resources/application.properties` and replace its contents with the connection details for your `jakone-database` Docker container:

  

Properties

  

```

spring.application.name=jakone-be

  

server.port=8080

  

# Environment variables with fallbacks for local running vs. Docker

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/bank_dki_db}

spring.datasource.username=${SPRING_DATASOURCE_USERNAME:dev_user}

spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:dev_password}

spring.datasource.driver-class-name=org.postgresql.Driver

  

# JPA / Hibernate configuration

spring.jpa.hibernate.ddl-auto=none

spring.jpa.show-sql=true

spring.jpa.properties.hibernate.format_sql=true

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

```
### Step 2: Healthcheck Endpoint ( Test Run )

File: `src/main/java/com/bankdki/jakone_be/controller/HealthController.java`

Java

```
package com.bankdki.jakone_be.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "JakOne Middleware Engine");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
}
```

## Docker Setup and First Run

### A. Dockerfile Setup

File: `jakone-be/Dockerfile`

### A. Dockerfile

```
# Build Stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

```

### B. Docker Build & Run (Option 1: Full Compose Stack)

Run from the root directory containing `docker-compose.yml`:

PowerShell

```
# Build and start PostgreSQL + Spring Boot backend
docker compose up -d --build
```
### Healthcheck Test

PowerShell

```
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/health" -Method Get

```

**Expected Response:**

JSON

```
{
  "service": "JakOne Middleware Engine",
  "status": "UP",
  "version": "1.0.0"
}
```

### Step 3: Account Entity (`Account.java`)

File: `src/main/java/com/bankdki/jakone_be/entity/Account.java`

Java

```
package com.bankdki.jakone_be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dki_accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "customer_nik", nullable = false, length = 16)
    private String customerNik;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Account() {}

    public Account(String accountNumber, String customerNik, String customerName, BigDecimal balance) {
        this.accountNumber = accountNumber;
        this.customerNik = customerNik;
        this.customerName = customerName;
        this.balance = balance;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCustomerNik() { return customerNik; }
    public void setCustomerNik(String customerNik) { this.customerNik = customerNik; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

```

### Step 4: Repository Interface (`AccountRepository.java`)

File: `src/main/java/com/bankdki/jakone_be/repository/AccountRepository.java`

Java

```
package com.bankdki.jakone_be.repository;

import com.bankdki.jakone_be.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
}

```

### Step 5: DTOs & Service Layer

#### A. Registration Request DTO (`RegisterRequest.java`)

File: `src/main/java/com/bankdki/jakone_be/dto/RegisterRequest.java`

Java

```
package com.bankdki.jakone_be.dto;

import java.math.BigDecimal;

public class RegisterRequest {
    private String customerNik;
    private String customerName;
    private BigDecimal initialBalance;

    public String getCustomerNik() { return customerNik; }
    public void setCustomerNik(String customerNik) { this.customerNik = customerNik; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}

```

#### B. Service Business Logic (`AccountService.java`)

File: `src/main/java/com/bankdki/jakone_be/service/AccountService.java`

Java

```
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

```

### Step 6: REST Controller (`AccountController.java`)

File: `src/main/java/com/bankdki/jakone_be/controller/AccountController.java`

Java

```
package com.bankdki.jakone_be.controller;

import com.bankdki.jakone_be.dto.RegisterRequest;
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
}

```

## 🧪 Verification & Testing Commands

Run testing commands in PowerShell to verify Part 1 execution:

### 1. Register New Account (`POST`)

PowerShell

```
$body = @{
    customerNik    = "3171012345670099"
    customerName   = "Budi Perdana"
    initialBalance = 750000.00
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body

```

### 2. Fetch Account Details (`GET`)

PowerShell

```
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/accounts/DKI-1000001" -Method Get

```

## ✏️ Student Mini-Task

> **Task:** Notice that `createdAt` returns `null` in the registration response body when creating a new account.
> 
> **Challenge:** Modify `Account.java` or `AccountService.java` so that `createdAt` reflects the current timestamp in the returned JSON upon creation.
