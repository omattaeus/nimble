-- Fix balance column name from account_balance to balance
ALTER TABLE users CHANGE COLUMN account_balance balance DECIMAL(10,2) DEFAULT 0.00;
