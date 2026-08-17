# Database Repository (`jakone-database`)

This repository contains the containerized **PostgreSQL 16** database setup and **pgAdmin 8** web management client for the **JakOne Digital Express** training project.

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

---

## 📁 Repository Structure

```text
jakone-db/
├── docker-compose.yml        # Docker service definitions (Postgres & pgAdmin)
├── init/             # Startup SQL scripts
    └── 01-schema.sql         # Table schemas & auto-seed script
```
### 1. Start Database
```
docker compose up -d
```
Verify that both containers (`jakone-express-db` and `jakone-db-admin`) are healthy and running:
```
docker compose ps
```
### 2. Accessing pgAdmin Web UI

1.  Open your browser and navigate to: **`http://localhost:5050`**
    
2.  **Log in with default credentials:**
    
    -   **Email:** `admin@bankdki.co.id`
        
    -   **Password:** `admin_password`
        

### 3. Connecting pgAdmin to PostgreSQL

To connect pgAdmin to the database container:

1.  Click **"Add New Server"** on the pgAdmin Dashboard.
    
2.  **General Tab:**
    
    -   **Name:** `Bank DKI Local`
        
3.  **Connection Tab:**
    
    -   **Host name/address:** `jakone-express-db` 
        
    -   **Port:** `5432`
        
    -   **Maintenance database:** `bank_dki_db`
        
    -   **Username:** `dev_user`
        
    -   **Password:** `dev_password`
        
4.  Click **Save**.

## 📊 Database Schema & Mock Data Seed

### Manual Data Seed / Reset Script

If you need to reset the database and seed fresh mock records (10 Accounts + 10 Mutations), open the **Query Tool** inside pgAdmin and execute:

SQL

```
-- Clean up old mock entries if re-running
TRUNCATE dki_mutations, dki_accounts RESTART IDENTITY CASCADE;

-- Insert 10 Accounts (JakOne Customers)
INSERT INTO dki_accounts (account_number, customer_nik, customer_name, balance) VALUES
('DKI-1000001', '3171010000000001', 'Ahmad Fauzi', 2500000.00),
('DKI-1000002', '3171010000000002', 'Siti Rahmawati', 1250000.50),
('DKI-1000003', '3171010000000003', 'Budi Santoso', 5000000.00),
('DKI-1000004', '3171010000000004', 'Dewi Lestari', 750000.00),
('DKI-1000005', '3171010000000005', 'Eko Prasetyo', 3100000.25),
('DKI-1000006', '3171010000000006', 'Fitriani Hidayat', 4500000.00),
('DKI-1000007', '3171010000000007', 'Giri Wijaya', 900000.00),
('DKI-1000008', '3171010000000008', 'Hany Handayani', 6200000.00),
('DKI-1000009', '3171010000000009', 'Irfan Hakim', 1800000.00),
('DKI-1000010', '3171010000000010', 'Joko Susilo', 10500000.00);

-- Insert 10 Initial Mutation Ledger Records
INSERT INTO dki_mutations (account_number, transaction_type, channel, amount, resulting_balance, created_at) VALUES
('DKI-1000001', 'DEPOSIT', 'TRANSFER', 2500000.00, 2500000.00, NOW() - INTERVAL '10 days'),
('DKI-1000002', 'DEPOSIT', 'CASH', 1500000.00, 1500000.00, NOW() - INTERVAL '9 days'),
('DKI-1000002', 'WITHDRAWAL', 'QRIS', 249999.50, 1250000.50, NOW() - INTERVAL '8 days'),
('DKI-1000003', 'DEPOSIT', 'TRANSFER', 5000000.00, 5000000.00, NOW() - INTERVAL '7 days'),
('DKI-1000004', 'DEPOSIT', 'CASH', 1000000.00, 1000000.00, NOW() - INTERVAL '6 days'),
('DKI-1000004', 'WITHDRAWAL', 'CASH', 250000.00, 750000.00, NOW() - INTERVAL '5 days'),
('DKI-1000005', 'DEPOSIT', 'TRANSFER', 3100000.25, 3100000.25, NOW() - INTERVAL '4 days'),
('DKI-1000006', 'DEPOSIT', 'CASH', 4500000.00, 4500000.00, NOW() - INTERVAL '3 days'),
('DKI-1000007', 'DEPOSIT', 'TRANSFER', 1000000.00, 1000000.00, NOW() - INTERVAL '2 days'),
('DKI-1000007', 'WITHDRAWAL', 'QRIS', 100000.00, 900000.00, NOW() - INTERVAL '1 day');

```

## 🛑 Stopping & Cleaning Up

To stop the database containers without deleting data:

Bash

```
docker compose stop

```

To tear down containers and clear all persisted database data:

Bash

```
docker compose down -v
```

