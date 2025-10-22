-- =====================================================
-- Migration: MySQL Optimizations and Configuration
-- Version: V002
-- Description: Advanced MySQL configurations for performance and security
-- =====================================================

-- 1. Performance Optimizations
-- Connection Settings
SET GLOBAL max_connections = 500;
SET GLOBAL wait_timeout = 28800; -- 8 hours
SET GLOBAL interactive_timeout = 28800; -- 8 hours

-- 2. Security Enhancements
-- Disable LOCAL INFILE (prevents client-side file loading)
SET GLOBAL local_infile = OFF;

-- 3. Monitoring and Logging
-- Slow Query Log: Helps identify inefficient queries
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1; -- Log queries taking longer than 1 second
SET GLOBAL log_output = 'FILE';

-- General Query Log (Use with caution, can generate large logs)
SET GLOBAL general_log = OFF; -- Keep OFF in production unless debugging

-- 4. Stored Procedures for common operations
-- Stored Procedure for Processing Payments
DELIMITER //
CREATE PROCEDURE sp_process_payment (
    IN p_charge_id CHAR(36),
    IN p_payer_id CHAR(36),
    IN p_amount DECIMAL(10,2),
    IN p_method ENUM('BALANCE', 'CREDIT_CARD'),
    IN p_external_transaction_id VARCHAR(100),
    IN p_card_number VARCHAR(19),
    IN p_card_expiry VARCHAR(7),
    IN p_card_cvv VARCHAR(4),
    IN p_authorization_code VARCHAR(50)
)
BEGIN
    DECLARE v_payer_balance DECIMAL(10,2);
    DECLARE v_charge_amount DECIMAL(10,2);
    DECLARE v_recipient_id CHAR(36);
    DECLARE v_payment_status ENUM('PENDING', 'PROCESSED', 'FAILED') DEFAULT 'PENDING';
    DECLARE v_failure_reason VARCHAR(500);

    -- Start transaction
    START TRANSACTION;

    -- Get payer's current balance
    SELECT balance INTO v_payer_balance FROM users WHERE id = p_payer_id FOR UPDATE;

    -- Get charge details
    SELECT amount, recipient_id INTO v_charge_amount, v_recipient_id FROM charges WHERE id = p_charge_id;

    -- Check if payer has sufficient balance (if method is BALANCE)
    IF p_method = 'BALANCE' AND v_payer_balance < p_amount THEN
        SET v_payment_status = 'FAILED';
        SET v_failure_reason = 'Insufficient balance';
    ELSE
               -- Process payment
               INSERT INTO payments (id, charge_id, payer_id, amount, status, method, external_transaction_id, card_number, card_expiry, card_cvv, authorization_code)
               VALUES (UUID_TO_BIN(UUID()), p_charge_id, p_payer_id, p_amount, 'PROCESSED', p_method, p_external_transaction_id, p_card_number, p_card_expiry, p_card_cvv, p_authorization_code);

        -- Update payer's balance (if method is BALANCE)
        IF p_method = 'BALANCE' THEN
            UPDATE users SET balance = balance - p_amount WHERE id = p_payer_id;
        END IF;

        -- Update recipient's balance
        UPDATE users SET balance = balance + p_amount WHERE id = v_recipient_id;

        -- Update charge status
        UPDATE charges SET status = 'PAID', paid_at = CURRENT_TIMESTAMP WHERE id = p_charge_id;

        SET v_payment_status = 'PROCESSED';
    END IF;

           -- If payment failed, log it
           IF v_payment_status = 'FAILED' THEN
               INSERT INTO payments (id, charge_id, payer_id, amount, status, method, failure_reason)
               VALUES (UUID_TO_BIN(UUID()), p_charge_id, p_payer_id, p_amount, 'FAILED', p_method, v_failure_reason);
           END IF;

    -- Commit transaction
    COMMIT;

    SELECT v_payment_status AS payment_status, v_failure_reason AS failure_reason;

END //
DELIMITER ;

-- Stored Procedure for Processing Deposits
DELIMITER //
CREATE PROCEDURE sp_process_deposit (
    IN p_user_id CHAR(36),
    IN p_amount DECIMAL(10,2),
    IN p_external_transaction_id VARCHAR(100),
    IN p_authorization_code VARCHAR(50)
)
BEGIN
    DECLARE v_deposit_status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING';
    DECLARE v_failure_reason VARCHAR(500);

    -- Start transaction
    START TRANSACTION;

       -- Insert deposit record
       INSERT INTO deposits (id, user_id, amount, status, external_transaction_id, authorization_code)
       VALUES (UUID_TO_BIN(UUID()), p_user_id, p_amount, 'APPROVED', p_external_transaction_id, p_authorization_code);

    -- Update user's balance
    UPDATE users SET balance = balance + p_amount WHERE id = p_user_id;

    SET v_deposit_status = 'APPROVED';

    -- Commit transaction
    COMMIT;

    SELECT v_deposit_status AS deposit_status, v_failure_reason AS failure_reason;

END //
DELIMITER ;