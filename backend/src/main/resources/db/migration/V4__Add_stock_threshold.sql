-- Copyright (c) 2025 Etrex Kuo. All rights reserved.

-- Add stock_threshold column to products table
ALTER TABLE products ADD COLUMN stock_threshold INT DEFAULT 5;
