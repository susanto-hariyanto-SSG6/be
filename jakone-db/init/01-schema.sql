-- Create Accounts Table
CREATE TABLE IF NOT EXISTS dki_accounts (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE NOT NULL,
    customer_nik VARCHAR(16) NOT NULL,
    customer_name VARCHAR(100) NOT NULL,
    balance NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create Mutations Ledger Table
CREATE TABLE IF NOT EXISTS dki_mutations (
    id BIGSERIAL PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL REFERENCES dki_accounts(account_number),
    transaction_type VARCHAR(20) NOT NULL, -- 'DEPOSIT' or 'WITHDRAWAL'
    channel VARCHAR(20) NOT NULL,          -- 'CASH', 'TRANSFER', 'QRIS'
    amount NUMERIC(15, 2) NOT NULL,
    resulting_balance NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed Initial Mock Account for Day 1 FE / Mobile Testing
INSERT INTO dki_accounts (account_number, customer_nik, customer_name, balance)
VALUES ('DKI-1029384', '3171012345670001', 'Nasabah Sample DKI', 500000.00)
ON CONFLICT (account_number) DO NOTHING;

INSERT INTO dki_mutations (account_number, transaction_type, channel, amount, resulting_balance)
VALUES ('DKI-1029384', 'DEPOSIT', 'TRANSFER', 500000.00, 500000.00)
ON CONFLICT DO NOTHING;