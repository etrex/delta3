-- Add order_no column to orders table (nullable first)
ALTER TABLE orders ADD COLUMN order_no VARCHAR(255);

-- Update existing orders with generated order_no
UPDATE orders SET order_no = CONCAT('ORD-', id, '-', REPLACE(RANDOM_UUID(), '-', '')) WHERE order_no IS NULL;

-- Make order_no NOT NULL and UNIQUE
ALTER TABLE orders ALTER COLUMN order_no SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT uk_orders_order_no UNIQUE (order_no);

-- Create shippings table
CREATE TABLE shippings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    tracking_number VARCHAR(255),
    carrier VARCHAR(255),
    estimated_delivery TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- Create index on order_id
CREATE INDEX idx_shippings_order_id ON shippings(order_id);
