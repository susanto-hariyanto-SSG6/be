
## 🎯 Core System Use Cases

Your Spring Boot session implements the four core API contracts used across Next.js (FE) and Kotlin (Mobile):

1.  **User Registration (`POST /api/v1/accounts`):**
    
    -   **Flow:** Validates NIK/Customer Name $\rightarrow$ Inserts record into `dki_accounts` $\rightarrow$ Returns new account JSON.
        
2.  **Add Funds (`POST /api/v1/accounts/{accountNumber}/transact` with `type: "DEPOSIT"`):**
    
    -   **Flow:** Routes request to `DepositStrategy` Bean $\rightarrow$ Credits balance $\rightarrow$ Writes ledger entry into `dki_mutations`.
        
3.  **Withdraw Funds (`POST /api/v1/accounts/{accountNumber}/transact` with `type: "WITHDRAWAL"`):**
    
    -   **Flow:** Locks database row (`@Lock`) $\rightarrow$ Validates balance $\rightarrow$ Returns `BDKI-4001` error via `@RestControllerAdvice` if insufficient $\rightarrow$ Debits balance & records mutation if valid.
        
4.  **Mutation Report (`GET /api/v1/accounts/{accountNumber}/mutations`):**
    
    -   **Flow:** Fetches account profile & historical mutation ledger $\rightarrow$ Returns unified history array for Web and Mobile rendering.
        

## ⏱️ Master Schedule & Code Build Progression

### Part 1: Database Integration & Account CRUD (08:00 - 09:40)

**Primary Focus:** Spring Beans, Component Scanning, and Spring Data JPA

**Implements Use Case:** #1 (User Registration) & Account Query

-   **Key Concepts Covered:**
    
    -   Setting up `application.properties` to connect to `localhost:5432/bank_dki_db`.
        
    -   Mapping Java classes to relational tables using `@Entity` and `@Table(name = "dki_accounts")`.
        
    -   Abstracting SQL queries using `JpaRepository<Account, Long>`.
        
-   **Hands-on Building Steps:**
    
    1.  Define `Account` entity (`id`, `accountNumber`, `customerNik`, `customerName`, `balance`).
        
    2.  Create `AccountRepository` interface extending `JpaRepository`.
        
    3.  Implement `AccountService` with constructor injection for `AccountRepository`.
        
    4.  Expose `AccountController` endpoints (`POST /api/v1/accounts` and `GET /api/v1/accounts/{accountNumber}`).
        

### Part 2: Dynamic Transaction Strategy Engine (10:00 - 12:00)

**Primary Focus:** Inversion of Control (IoC), Beans, and Strategy Pattern

**Implements Use Case:** Routing for #2 (Add Funds) and #3 (Withdraw Funds)

-   **Key Concepts Covered:**
    
    -   Eliminating `if-else` or `switch` blocks using Spring-managed Beans.
        
    -   Spring's automatic map injection: `Map<String, TransactionStrategy>`.
        
    -   DTO vs. Entity boundary separation.
        
-   **Hands-on Building Steps:**
    
    1.  Define `TransactionStrategy` interface:
        
        Java
        
        ```
        public interface TransactionStrategy {
            void execute(Account account, BigDecimal amount, String channel);
        }
        
        ```
        
    2.  Implement concrete Spring Beans:
        
        -   `@Component("DEPOSIT")` $\rightarrow$ `DepositStrategy` (credits balance).
            
        -   `@Component("WITHDRAWAL")` $\rightarrow$ `WithdrawalStrategy` (debits balance).
            
    3.  Inject `Map<String, TransactionStrategy>` into `AccountService` to dynamically select execution logic based on the incoming request payload string.
        

### ☕ Lunch Break (12:30 - 13:30)

### Part 3: Atomic Transactions & Mutation Ledger (13:00 - 14:40)

**Primary Focus:** `@Transactional` boundaries, Database Locks, and Audit Logging

**Implements Use Case:** #2 & #3 (Ledger Writing) and #4 (Mutation Report)

-   **Key Concepts Covered:**
    
    -   Understanding Spring's Dynamic Proxy mechanism for `@Transactional`.
        
    -   Pessimistic DB Write Locks (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) to prevent race conditions during high concurrency.
        
    -   Atomic operations: Updating `dki_accounts` and inserting into `dki_mutations` in a single transaction.
        
-   **Hands-on Building Steps:**
    
    1.  Define `Mutation` entity mapped to `dki_mutations`.
        
    2.  Add pessimistic locking query in `AccountRepository`:
        
        Java
        
        ```
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
        Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
        
        ```
        
    3.  Create `executeTransaction()` service method wrapped in `@Transactional` that saves both balance updates and mutation records atomically.
        
    4.  Implement `GET /api/v1/accounts/{accountNumber}/mutations` endpoint.
        

### Part 4: Banking Exception Contracts & Audit Filter (15:00 - 17:00)

**Primary Focus:** Global Exception Handling and Security Interceptors

**Implements Use Case:** Error Contracts & Multi-Channel Headers

-   **Key Concepts Covered:**
    
    -   Centralized Exception Handling using `@RestControllerAdvice`.
        
    -   Banking API Response Standards (`BDKI-4001` Insufficient Balance).
        
    -   Request interceptor patterns using `OncePerRequestFilter` and `MDC` logging.
        
-   **Hands-on Building Steps:**
    
    1.  Create custom exception classes (`InsufficientBalanceException`, `AccountNotFoundException`).
        
    2.  Build `GlobalExceptionHandler` with `@ExceptionHandler` methods to convert raw exceptions into structured JSON error contracts.
        
    3.  Implement a custom `OncePerRequestFilter` to extract `X-Channel-ID` (`WEB` vs `MOBILE`) and `X-Trace-ID` HTTP headers for MDC audit logging.
        
    4.  **Live Integration Check (16:30):** Connect the Day 1 Next.js frontend to the Spring Boot backend and run an end-to-end deposit/withdrawal test.
        

