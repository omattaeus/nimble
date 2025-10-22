-- Add payment_date column to payments table
ALTER TABLE payments ADD COLUMN payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Update existing records to have current timestamp as payment date
UPDATE payments SET payment_date = CURRENT_TIMESTAMP WHERE payment_date IS NULL;

-- Make the column NOT NULL after setting default values
ALTER TABLE payments MODIFY COLUMN payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
