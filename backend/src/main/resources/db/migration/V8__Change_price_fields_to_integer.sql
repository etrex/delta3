-- Copyright (c) 2025 Etrex Kuo. All rights reserved.

-- Change price fields from DECIMAL to INTEGER
ALTER TABLE products MODIFY COLUMN price INT NOT NULL;
ALTER TABLE order_items MODIFY COLUMN price INT NOT NULL;
ALTER TABLE orders MODIFY COLUMN total_amount INT NOT NULL;
ALTER TABLE payments MODIFY COLUMN amount INT NOT NULL;
