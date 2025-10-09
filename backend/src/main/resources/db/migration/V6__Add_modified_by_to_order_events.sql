-- Add modified_by column to order_events table for audit trail
ALTER TABLE order_events ADD COLUMN modified_by BIGINT;
ALTER TABLE order_events ADD CONSTRAINT fk_order_events_modified_by FOREIGN KEY (modified_by) REFERENCES users(id);
CREATE INDEX idx_order_events_modified_by ON order_events(modified_by);
