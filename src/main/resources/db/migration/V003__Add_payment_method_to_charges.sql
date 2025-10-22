-- Add payment_method column to charges table
ALTER TABLE charges ADD COLUMN payment_method VARCHAR(20) DEFAULT 'BALANCE';

-- Update existing records to have BALANCE as default payment method
UPDATE charges SET payment_method = 'BALANCE' WHERE payment_method IS NULL;

-- Make the column NOT NULL after setting default values
ALTER TABLE charges MODIFY COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'BALANCE';
