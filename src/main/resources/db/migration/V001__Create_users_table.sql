-- Create users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    account_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    
    INDEX idx_user_cpf (cpf),
    INDEX idx_user_email (email),
    INDEX idx_user_active (is_active)
);

-- Create charges table
CREATE TABLE charges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    originator_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    status ENUM('PENDING', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    paid_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    
    FOREIGN KEY (originator_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_charge_originator (originator_id),
    INDEX idx_charge_recipient (recipient_id),
    INDEX idx_charge_status (status),
    INDEX idx_charge_created_at (created_at)
);

-- Create payments table
CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    charge_id BIGINT NOT NULL,
    payer_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'PROCESSED', 'FAILED') NOT NULL DEFAULT 'PENDING',
    method ENUM('BALANCE', 'CREDIT_CARD') NOT NULL,
    external_transaction_id VARCHAR(100),
    card_number VARCHAR(19),
    card_expiry VARCHAR(7),
    card_cvv VARCHAR(4),
    authorization_code VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    failed_at TIMESTAMP NULL,
    failure_reason VARCHAR(500),
    
    FOREIGN KEY (charge_id) REFERENCES charges(id) ON DELETE CASCADE,
    FOREIGN KEY (payer_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_payment_charge (charge_id),
    INDEX idx_payment_payer (payer_id),
    INDEX idx_payment_status (status),
    INDEX idx_payment_created_at (created_at),
    INDEX idx_payment_external_id (external_transaction_id)
);
