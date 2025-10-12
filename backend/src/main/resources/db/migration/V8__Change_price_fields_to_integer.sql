-- Copyright (c) 2025 Etrex Kuo. All rights reserved.

-- Change price fields from DECIMAL to INTEGER (H2 syntax)
ALTER TABLE products ALTER COLUMN price INT NOT NULL;
ALTER TABLE order_items ALTER COLUMN price INT NOT NULL;
ALTER TABLE orders ALTER COLUMN total_amount INT NOT NULL;
ALTER TABLE payments ALTER COLUMN amount INT NOT NULL;
