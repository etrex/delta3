-- Add failure_reason column to payments table
ALTER TABLE payments ADD COLUMN failure_reason VARCHAR(500);
